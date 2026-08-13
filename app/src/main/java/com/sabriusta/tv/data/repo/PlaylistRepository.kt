package com.sabriusta.tv.data.repo

import android.net.Uri
import com.sabriusta.tv.core.UrlValidator
import com.sabriusta.tv.data.catalog.StarterCatalog
import com.sabriusta.tv.data.local.CategoryEntity
import com.sabriusta.tv.data.local.CategoryDao
import com.sabriusta.tv.data.local.MediaItemDao
import com.sabriusta.tv.data.local.MediaItemEntity
import com.sabriusta.tv.data.local.MediaType
import com.sabriusta.tv.data.local.PlaylistDao
import com.sabriusta.tv.data.local.PlaylistEntity
import com.sabriusta.tv.data.local.SourceType
import com.sabriusta.tv.data.m3u.M3uEntry
import com.sabriusta.tv.data.m3u.M3uMediaType
import com.sabriusta.tv.data.m3u.M3uParser
import com.sabriusta.tv.data.prefs.SettingsRepository
import com.sabriusta.tv.data.remote.PlaylistFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

data class ImportOutcome(
    val success: Boolean,
    val message: String,
    val tvCount: Int = 0,
    val radioCount: Int = 0,
    val movieCount: Int = 0,
    val skipped: Int = 0,
    val duplicates: Int = 0
)

@Singleton
class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val mediaItemDao: MediaItemDao,
    private val categoryDao: CategoryDao,
    private val fetcher: PlaylistFetcher,
    private val settings: SettingsRepository,
    private val starterCatalog: StarterCatalog
) {
    fun observePlaylists(): Flow<List<PlaylistEntity>> = playlistDao.observeAll()

    suspend fun addFromUrl(name: String, url: String, autoUpdate: Boolean): ImportOutcome {
        val allowHttp = settings.current().allowHttp
        val validation = UrlValidator.validate(url, allowHttp)
        if (validation is UrlValidator.Result.Invalid) {
            return ImportOutcome(false, validation.reason)
        }
        val playlistId = playlistDao.insert(
            PlaylistEntity(
                name = name.ifBlank { "Liste ${System.currentTimeMillis() % 1000}" },
                source = url.trim(),
                sourceType = SourceType.URL,
                autoUpdate = autoUpdate,
                enabled = true
            )
        )
        return refresh(playlistId)
    }

    suspend fun addFromUri(name: String, uri: Uri): ImportOutcome {
        val result = fetcher.readFromUri(uri)
        if (result is PlaylistFetcher.FetchResult.Failure) {
            return ImportOutcome(false, result.message)
        }
        val content = (result as PlaylistFetcher.FetchResult.Success).content
        val playlistId = playlistDao.insert(
            PlaylistEntity(
                name = name.ifBlank { "Dosyadan liste" },
                source = uri.toString(),
                sourceType = SourceType.FILE,
                enabled = true
            )
        )
        return importContent(playlistId, content)
    }

    suspend fun addFromText(name: String, text: String): ImportOutcome {
        if (text.isBlank()) return ImportOutcome(false, "Yapistirilan metin bos.")
        val playlistId = playlistDao.insert(
            PlaylistEntity(
                name = name.ifBlank { "Yapistirilan liste" },
                source = "text",
                sourceType = SourceType.TEXT,
                enabled = true
            )
        )
        return importContent(playlistId, text)
    }

    suspend fun refresh(playlistId: Long): ImportOutcome {
        val playlist = playlistDao.byId(playlistId)
            ?: return ImportOutcome(false, "Liste bulunamadi.")
        if (playlist.sourceType == SourceType.TEXT) {
            return ImportOutcome(false, "Yapistirilan listeler yenilenemez, yeniden ekleyin.")
        }
        val result = when (playlist.sourceType) {
            SourceType.URL -> fetcher.fetchFromUrl(playlist.source, settings.current().allowHttp)
            SourceType.FILE -> fetcher.readFromUri(Uri.parse(playlist.source))
            else -> PlaylistFetcher.FetchResult.Failure("Bu liste turu yenilemeyi desteklemiyor.")
        }
        return when (result) {
            is PlaylistFetcher.FetchResult.Failure -> {
                playlistDao.markUpdated(playlistId, playlist.itemCount, playlist.lastUpdatedAt, result.message)
                ImportOutcome(false, result.message)
            }
            is PlaylistFetcher.FetchResult.Success -> importContent(playlistId, result.content)
        }
    }

    suspend fun importContent(playlistId: Long, content: String): ImportOutcome = withContext(Dispatchers.Default) {
        val deduplicate = settings.current().deduplicate
        val parsed = M3uParser.parse(content, deduplicate)
        if (parsed.entries.isEmpty()) {
            val message = "Listede oynatilabilir yayin bulunamadi."
            playlistDao.markUpdated(playlistId, 0, System.currentTimeMillis(), message)
            return@withContext ImportOutcome(false, message, skipped = parsed.skippedLines.size)
        }

        val now = System.currentTimeMillis()
        val items = parsed.entries.mapIndexed { index, entry -> entry.toEntity(playlistId, now, index) }

        mediaItemDao.deleteByPlaylist(playlistId)
        // Buyuk listeler arayuzu dondurmasin diye parca parca yazilir.
        items.chunked(500).forEach { chunk -> mediaItemDao.insertAll(chunk) }
        rebuildCategories()
        playlistDao.markUpdated(playlistId, items.size, now, null)

        ImportOutcome(
            success = true,
            message = "${items.size} yayin eklendi.",
            tvCount = parsed.tvCount,
            radioCount = parsed.radioCount,
            movieCount = parsed.movieCount,
            skipped = parsed.skippedLines.size,
            duplicates = parsed.duplicatesRemoved
        )
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) = playlistDao.setEnabled(id, enabled)

    suspend fun setAutoUpdate(id: Long, auto: Boolean) = playlistDao.setAutoUpdate(id, auto)

    suspend fun rename(id: Long, name: String) {
        val existing = playlistDao.byId(id) ?: return
        playlistDao.update(existing.copy(name = name))
    }

    suspend fun delete(id: Long) {
        mediaItemDao.deleteByPlaylist(id)
        playlistDao.delete(id)
        rebuildCategories()
    }

    suspend fun refreshAutoUpdatable() {
        playlistDao.enabledPlaylists()
            .filter { it.autoUpdate && it.sourceType == SourceType.URL }
            .forEach { runCatching { refresh(it.id) } }
    }

    /** Ilk acilista dogrulanmis ornek katalogu yerel liste olarak yukler. */
    suspend fun seedStarterCatalogIfNeeded() {
        if (settings.current().catalogSeeded) return
        val catalog = starterCatalog.load()
        if (catalog.sources.isEmpty()) {
            settings.setCatalogSeeded(true)
            return
        }
        val now = System.currentTimeMillis()
        val playlistId = playlistDao.insert(
            PlaylistEntity(
                name = "Ornek Katalog (dogrulanmis kaynaklar)",
                source = "starter_catalog.json",
                sourceType = SourceType.CATALOG,
                enabled = true,
                isBuiltIn = true,
                lastUpdatedAt = now
            )
        )
        val items = catalog.sources.mapIndexed { index, source ->
            MediaItemEntity(
                id = stableId(source.url, source.name),
                playlistId = playlistId,
                name = source.name,
                url = source.url,
                logoUrl = source.logo.ifBlank { null },
                category = source.category.ifBlank { "Genel" },
                type = when (source.type.uppercase()) {
                    "RADIO" -> MediaType.RADIO
                    "MOVIE" -> MediaType.MOVIE
                    else -> MediaType.TV
                },
                description = buildString {
                    if (source.publisher.isNotBlank()) append("Yayinci: ${source.publisher}. ")
                    if (source.country.isNotBlank()) append("Ulke: ${source.country}. ")
                    if (source.license.isNotBlank()) append(source.license)
                }.trim().ifBlank { null },
                addedAt = now,
                sortIndex = index
            )
        }
        mediaItemDao.insertAll(items)
        playlistDao.markUpdated(playlistId, items.size, now, null)
        rebuildCategories()
        settings.setCatalogSeeded(true)
    }

    private suspend fun rebuildCategories() {
        listOf(MediaType.TV, MediaType.RADIO, MediaType.MOVIE).forEach { type ->
            val items = mediaItemDao.listByType(type)
            val categories = items.groupingBy { it.category }.eachCount()
                .map { (name, count) -> CategoryEntity(name = name, type = type, itemCount = count) }
            categoryDao.deleteByType(type)
            if (categories.isNotEmpty()) categoryDao.insertAll(categories)
        }
    }

    companion object {
        fun stableId(url: String, name: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest("${url.trim().lowercase()}|${name.trim().lowercase()}".toByteArray())
            return bytes.take(16).joinToString("") { "%02x".format(it) }
        }
    }
}

private fun M3uEntry.toEntity(playlistId: Long, now: Long, index: Int): MediaItemEntity = MediaItemEntity(
    id = PlaylistRepository.stableId(url, name),
    playlistId = playlistId,
    name = name,
    url = url,
    logoUrl = logoUrl,
    category = group,
    type = when (type) {
        M3uMediaType.RADIO -> MediaType.RADIO
        M3uMediaType.MOVIE -> MediaType.MOVIE
        M3uMediaType.TV -> MediaType.TV
    },
    tvgId = tvgId,
    durationMs = durationSeconds?.times(1000L),
    addedAt = now,
    sortIndex = index
)
