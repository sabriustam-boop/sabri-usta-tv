package com.sabriusta.tv.ui.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sabriusta.tv.data.local.FavoriteEntity
import com.sabriusta.tv.data.prefs.AppSettings
import com.sabriusta.tv.data.prefs.OrientationMode
import com.sabriusta.tv.data.prefs.SettingsRepository
import com.sabriusta.tv.data.prefs.ThemeMode
import com.sabriusta.tv.data.repo.MediaRepository
import com.sabriusta.tv.data.repo.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
data class FavoriteBackup(
    val itemId: String,
    val name: String,
    val url: String,
    val logoUrl: String? = null,
    val type: String,
    val category: String = "Genel"
)

@Serializable
data class PlaylistBackup(
    val name: String,
    val source: String,
    val sourceType: String
)

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val mediaRepository: MediaRepository,
    private val playlistRepository: PlaylistRepository,
    private val contentResolver: ContentResolver
) : ViewModel() {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val message = MutableStateFlow<String?>(null)

    val state: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        message
    ) { settings, msg -> SettingsUiState(settings, msg) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    fun setOrientation(mode: OrientationMode) = viewModelScope.launch { settingsRepository.setOrientationMode(mode) }
    fun setAutoFullscreen(value: Boolean) = viewModelScope.launch { settingsRepository.setAutoFullscreen(value) }
    fun setWifiOnly(value: Boolean) = viewModelScope.launch { settingsRepository.setWifiOnly(value) }
    fun setMobileWarning(value: Boolean) = viewModelScope.launch { settingsRepository.setMobileDataWarning(value) }
    fun setBuffer(value: Int) = viewModelScope.launch { settingsRepository.setBufferSeconds(value) }
    fun setDeduplicate(value: Boolean) = viewModelScope.launch { settingsRepository.setDeduplicate(value) }

    fun setAllowHttp(value: Boolean) = viewModelScope.launch {
        settingsRepository.setAllowHttp(value)
        message.value = if (value) {
            "HTTP yayinlarina izin verildi. Sifresiz baglantilar ag uzerinde izlenebilir; yalnizca guvendiginiz kaynaklarda kullanin."
        } else {
            "HTTP yayinlari yeniden engellendi."
        }
    }

    fun clearHistory() = viewModelScope.launch {
        mediaRepository.clearHistory()
        message.value = "Izleme gecmisi ve ilerleme kayitlari silindi."
    }

    fun exportFavorites(uri: Uri) = viewModelScope.launch {
        val favorites = mediaRepository.exportFavorites().map {
            FavoriteBackup(it.itemId, it.name, it.url, it.logoUrl, it.type, it.category)
        }
        val text = json.encodeToString(ListSerializer(FavoriteBackup.serializer()), favorites)
        message.value = if (writeText(uri, text)) {
            "${favorites.size} favori disa aktarildi."
        } else {
            "Favoriler disa aktarilamadi."
        }
    }

    fun importFavorites(uri: Uri) = viewModelScope.launch {
        val text = readText(uri)
        if (text == null) {
            message.value = "Dosya okunamadi."
            return@launch
        }
        val parsed = runCatching {
            json.decodeFromString(ListSerializer(FavoriteBackup.serializer()), text)
        }.getOrNull()
        if (parsed == null) {
            message.value = "Dosya bicimi taninmadi."
            return@launch
        }
        mediaRepository.importFavorites(
            parsed.map {
                FavoriteEntity(
                    itemId = it.itemId,
                    name = it.name,
                    url = it.url,
                    logoUrl = it.logoUrl,
                    type = it.type,
                    category = it.category,
                    addedAt = System.currentTimeMillis()
                )
            }
        )
        message.value = "${parsed.size} favori ice aktarildi."
    }

    fun exportPlaylists(uri: Uri) = viewModelScope.launch {
        // Anlik goruntu almak icin tek seferlik okuma yapilir.
        val snapshot = playlistRepository.observePlaylists().first()
            .filterNot { it.isBuiltIn }
            .map { PlaylistBackup(it.name, it.source, it.sourceType) }
        val text = json.encodeToString(ListSerializer(PlaylistBackup.serializer()), snapshot)
        message.value = if (writeText(uri, text)) {
            "${snapshot.size} liste disa aktarildi. Dosya hassas baglanti icerebilir, guvenli saklayin."
        } else {
            "Listeler disa aktarilamadi."
        }
    }

    fun importPlaylists(uri: Uri) = viewModelScope.launch {
        val text = readText(uri)
        if (text == null) {
            message.value = "Dosya okunamadi."
            return@launch
        }
        val parsed = runCatching {
            json.decodeFromString(ListSerializer(PlaylistBackup.serializer()), text)
        }.getOrNull()
        if (parsed == null) {
            message.value = "Dosya bicimi taninmadi."
            return@launch
        }
        var added = 0
        parsed.forEach { backup ->
            val outcome = playlistRepository.addFromUrl(backup.name, backup.source, autoUpdate = false)
            if (outcome.success) added++
        }
        message.value = "$added liste ice aktarildi."
    }

    fun clearMessage() { message.value = null }

    private suspend fun writeText(uri: Uri, text: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
            true
        }.getOrDefault(false)
    }

    private suspend fun readText(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
        }.getOrNull()
    }
}
