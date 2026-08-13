package com.sabriusta.tv.data.repo

import com.sabriusta.tv.data.local.CustomSourceDao
import com.sabriusta.tv.data.local.CustomSourceEntity
import com.sabriusta.tv.data.local.FavoriteDao
import com.sabriusta.tv.data.local.FavoriteEntity
import com.sabriusta.tv.data.local.MediaItemDao
import com.sabriusta.tv.data.local.MediaItemEntity
import com.sabriusta.tv.data.local.MediaType
import com.sabriusta.tv.data.local.PlaybackProgressDao
import com.sabriusta.tv.data.local.PlaybackProgressEntity
import com.sabriusta.tv.data.local.RadioHistoryDao
import com.sabriusta.tv.data.local.RadioHistoryEntity
import com.sabriusta.tv.data.local.SourceType
import com.sabriusta.tv.data.local.WatchHistoryDao
import com.sabriusta.tv.data.local.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Oynaticiya verilecek sadelestirilmis icerik modeli. */
data class PlayableItem(
    val id: String,
    val name: String,
    val url: String,
    val logoUrl: String?,
    val type: String,
    val category: String = "Genel",
    val description: String? = null,
    val isLocalFile: Boolean = false
)

@Singleton
class MediaRepository @Inject constructor(
    private val mediaItemDao: MediaItemDao,
    private val favoriteDao: FavoriteDao,
    private val watchHistoryDao: WatchHistoryDao,
    private val progressDao: PlaybackProgressDao,
    private val radioHistoryDao: RadioHistoryDao,
    private val customSourceDao: CustomSourceDao
) {
    fun observeByType(type: String): Flow<List<MediaItemEntity>> = mediaItemDao.observeByType(type)

    fun search(type: String, category: String, query: String): Flow<List<MediaItemEntity>> =
        mediaItemDao.search(type, category, query)

    fun observeCategories(type: String): Flow<List<String>> = mediaItemDao.observeCategories(type)

    fun observeCount(type: String): Flow<Int> = mediaItemDao.observeCount(type)

    fun observeRecentlyAdded(limit: Int = 20): Flow<List<MediaItemEntity>> =
        mediaItemDao.observeRecentlyAdded(limit)

    fun observeFavorites(): Flow<List<FavoriteEntity>> = favoriteDao.observeAll()

    fun observeFavoritesByType(type: String): Flow<List<FavoriteEntity>> = favoriteDao.observeByType(type)

    fun observeFavoriteIds(): Flow<List<String>> = favoriteDao.observeIds()

    fun observeHistory(limit: Int = 30): Flow<List<WatchHistoryEntity>> = watchHistoryDao.observeRecent(limit)

    fun observeLastWatched(type: String): Flow<WatchHistoryEntity?> = watchHistoryDao.observeLast(type)

    fun observeRadioHistory(): Flow<List<RadioHistoryEntity>> = radioHistoryDao.observeRecent()

    fun observeContinueWatching(): Flow<List<PlaybackProgressEntity>> = progressDao.observeContinueWatching()

    fun observeCustomSources(type: String): Flow<List<CustomSourceEntity>> = customSourceDao.observeByType(type)

    suspend fun toggleFavorite(item: PlayableItem): Boolean {
        val isFav = favoriteDao.isFavorite(item.id)
        if (isFav) {
            favoriteDao.delete(item.id)
        } else {
            favoriteDao.insert(
                FavoriteEntity(
                    itemId = item.id,
                    name = item.name,
                    url = item.url,
                    logoUrl = item.logoUrl,
                    type = item.type,
                    category = item.category,
                    addedAt = System.currentTimeMillis()
                )
            )
        }
        return !isFav
    }

    suspend fun isFavorite(id: String): Boolean = favoriteDao.isFavorite(id)

    suspend fun exportFavorites(): List<FavoriteEntity> = favoriteDao.listAll()

    suspend fun importFavorites(favorites: List<FavoriteEntity>) = favoriteDao.insertAll(favorites)

    suspend fun recordPlayback(item: PlayableItem) {
        val now = System.currentTimeMillis()
        watchHistoryDao.insert(
            WatchHistoryEntity(
                itemId = item.id,
                name = item.name,
                url = item.url,
                logoUrl = item.logoUrl,
                type = item.type,
                lastPlayedAt = now
            )
        )
        if (item.type == MediaType.RADIO) {
            radioHistoryDao.insert(
                RadioHistoryEntity(
                    itemId = item.id,
                    name = item.name,
                    url = item.url,
                    logoUrl = item.logoUrl,
                    lastPlayedAt = now
                )
            )
        }
    }

    suspend fun saveProgress(itemId: String, positionMs: Long, durationMs: Long) {
        if (positionMs <= 0) return
        progressDao.upsert(
            PlaybackProgressEntity(
                itemId = itemId,
                positionMs = positionMs,
                durationMs = durationMs,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun progressOf(itemId: String): Long = progressDao.byId(itemId)?.positionMs ?: 0L

    suspend fun addCustomSource(name: String, url: String, type: String, isLocalFile: Boolean): CustomSourceEntity {
        val entity = CustomSourceEntity(
            id = PlaylistRepository.stableId(url, name),
            name = name,
            url = url,
            type = type,
            sourceType = if (isLocalFile) SourceType.LOCAL_VIDEO else SourceType.DIRECT,
            addedAt = System.currentTimeMillis()
        )
        customSourceDao.insert(entity)
        return entity
    }

    suspend fun deleteCustomSource(id: String) = customSourceDao.delete(id)

    suspend fun clearHistory() {
        watchHistoryDao.clear()
        radioHistoryDao.clear()
        progressDao.clear()
    }

    /** Oynaticinin id ile icerik bulmasi icin: once liste, sonra ozel kaynak, sonra gecmis. */
    suspend fun findPlayable(id: String): PlayableItem? {
        mediaItemDao.byId(id)?.let { return it.toPlayable() }
        customSourceDao.byId(id)?.let { return it.toPlayable() }
        return null
    }

    /** Sonraki/onceki yayin icin ayni turdeki siralanmis listeyi dondurur. */
    suspend fun queueForType(type: String): List<PlayableItem> =
        mediaItemDao.listByType(type).map { it.toPlayable() }
}

fun MediaItemEntity.toPlayable() = PlayableItem(
    id = id,
    name = name,
    url = url,
    logoUrl = logoUrl,
    type = type,
    category = category,
    description = description
)

fun FavoriteEntity.toPlayable() = PlayableItem(
    id = itemId,
    name = name,
    url = url,
    logoUrl = logoUrl,
    type = type,
    category = category
)

fun WatchHistoryEntity.toPlayable() = PlayableItem(
    id = itemId,
    name = name,
    url = url,
    logoUrl = logoUrl,
    type = type
)

fun RadioHistoryEntity.toPlayable() = PlayableItem(
    id = itemId,
    name = name,
    url = url,
    logoUrl = logoUrl,
    type = MediaType.RADIO
)

fun CustomSourceEntity.toPlayable() = PlayableItem(
    id = id,
    name = name,
    url = url,
    logoUrl = logoUrl,
    type = type,
    description = description,
    isLocalFile = sourceType == SourceType.LOCAL_VIDEO
)
