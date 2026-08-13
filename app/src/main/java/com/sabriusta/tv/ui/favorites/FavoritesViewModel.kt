package com.sabriusta.tv.ui.favorites

import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.sabriusta.tv.data.local.FavoriteEntity
import com.sabriusta.tv.data.local.MediaType
import com.sabriusta.tv.data.repo.MediaRepository
import com.sabriusta.tv.data.repo.toPlayable
import com.sabriusta.tv.player.RadioController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesUiState(
    val favorites: List<FavoriteEntity> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(UnstableApi::class)
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val radioController: RadioController
) : ViewModel() {

    val state: StateFlow<FavoritesUiState> = repository.observeFavorites()
        .map { FavoritesUiState(favorites = it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FavoritesUiState())

    fun remove(item: FavoriteEntity) {
        viewModelScope.launch { repository.toggleFavorite(item.toPlayable()) }
    }

    /** Radyo favorileri dogrudan arka plan servisinde calar. */
    fun playRadio(item: FavoriteEntity) {
        viewModelScope.launch {
            val playable = item.toPlayable()
            radioController.play(playable)
            repository.recordPlayback(playable)
        }
    }

    fun isRadio(item: FavoriteEntity): Boolean = item.type == MediaType.RADIO
}
