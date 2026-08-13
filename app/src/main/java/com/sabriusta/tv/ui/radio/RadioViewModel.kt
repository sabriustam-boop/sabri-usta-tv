package com.sabriusta.tv.ui.radio

import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.sabriusta.tv.data.local.MediaItemEntity
import com.sabriusta.tv.data.local.MediaType
import com.sabriusta.tv.data.prefs.SettingsRepository
import com.sabriusta.tv.core.NetworkMonitor
import com.sabriusta.tv.core.PlaybackErrors
import com.sabriusta.tv.data.repo.MediaRepository
import com.sabriusta.tv.data.repo.toPlayable
import com.sabriusta.tv.player.RadioController
import com.sabriusta.tv.player.RadioState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RadioUiState(
    val items: List<MediaItemEntity> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String = "",
    val query: String = "",
    val favoriteIds: Set<String> = emptySet(),
    val lastPlayedName: String? = null,
    val lastPlayedId: String? = null,
    val player: RadioState = RadioState(),
    val blockedMessage: String? = null,
    val isLoading: Boolean = true
)

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RadioViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val radioController: RadioController,
    private val settings: SettingsRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val category = MutableStateFlow("")
    private val blocked = MutableStateFlow<String?>(null)

    private val filtered = combine(query, category) { q, c -> q to c }
        .flatMapLatest { (q, c) -> repository.search(MediaType.RADIO, c, q) }

    private val listData = combine(
        filtered,
        repository.observeCategories(MediaType.RADIO),
        repository.observeFavoriteIds(),
        repository.observeRadioHistory()
    ) { items, categories, favorites, history ->
        Quad(items, categories, favorites.toSet(), history.firstOrNull())
    }

    val state: StateFlow<RadioUiState> = combine(
        listData,
        radioController.state,
        combine(query, category) { q, c -> q to c },
        blocked
    ) { data, playerState, filters, blockedMessage ->
        RadioUiState(
            items = data.items,
            categories = data.categories,
            selectedCategory = filters.second,
            query = filters.first,
            favoriteIds = data.favorites,
            lastPlayedName = data.lastPlayed?.name,
            lastPlayedId = data.lastPlayed?.itemId,
            player = playerState,
            blockedMessage = blockedMessage,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RadioUiState())

    private data class Quad(
        val items: List<MediaItemEntity>,
        val categories: List<String>,
        val favorites: Set<String>,
        val lastPlayed: com.sabriusta.tv.data.local.RadioHistoryEntity?
    )

    fun onQueryChange(value: String) { query.value = value }
    fun onCategorySelect(value: String) { category.value = value }

    fun toggleFavorite(item: MediaItemEntity) {
        viewModelScope.launch { repository.toggleFavorite(item.toPlayable()) }
    }

    fun play(item: MediaItemEntity) {
        viewModelScope.launch {
            val network = networkMonitor.currentState()
            if (!network.isOnline) {
                blocked.value = PlaybackErrors.NO_INTERNET
                return@launch
            }
            if (settings.current().wifiOnly && !network.isUnmetered) {
                blocked.value = PlaybackErrors.WIFI_ONLY
                return@launch
            }
            blocked.value = null
            val playable = item.toPlayable()
            radioController.play(playable)
            repository.recordPlayback(playable)
        }
    }

    fun playById(id: String) {
        viewModelScope.launch {
            val playable = repository.findPlayable(id) ?: return@launch
            radioController.play(playable)
            repository.recordPlayback(playable)
        }
    }

    fun togglePlayPause() {
        viewModelScope.launch { radioController.togglePlayPause() }
    }

    fun stop() {
        viewModelScope.launch { radioController.stop() }
    }

    fun setSleepTimer(minutes: Int) {
        viewModelScope.launch { radioController.setSleepTimer(minutes) }
    }

    fun clearMessages() {
        blocked.value = null
        radioController.clearError()
    }
}
