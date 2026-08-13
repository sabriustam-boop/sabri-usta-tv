package com.sabriusta.tv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sabriusta.tv.data.local.MediaType
import com.sabriusta.tv.data.repo.MediaRepository
import com.sabriusta.tv.data.repo.PlayableItem
import com.sabriusta.tv.data.repo.toPlayable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ContinueItem(
    val item: PlayableItem,
    val percent: Int
)

data class HomeUiState(
    val continueWatching: List<ContinueItem> = emptyList(),
    val favorites: List<PlayableItem> = emptyList(),
    val recentlyAdded: List<PlayableItem> = emptyList(),
    val recentlyPlayed: List<PlayableItem> = emptyList(),
    val tvCount: Int = 0,
    val radioCount: Int = 0,
    val movieCount: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val counts = combine(
        repository.observeCount(MediaType.TV),
        repository.observeCount(MediaType.RADIO),
        repository.observeCount(MediaType.MOVIE)
    ) { tv, radio, movie -> Triple(tv, radio, movie) }

    private val lists = combine(
        repository.observeFavorites().map { list -> list.map { it.toPlayable() } },
        repository.observeRecentlyAdded(20).map { list -> list.map { it.toPlayable() } },
        repository.observeHistory(20).map { list -> list.map { it.toPlayable() } }
    ) { favorites, added, played -> Triple(favorites, added, played) }

    val state: StateFlow<HomeUiState> = combine(
        counts,
        lists,
        repository.observeContinueWatching(),
        repository.observeHistory(50)
    ) { countTriple, listTriple, progress, history ->
        val historyById = history.associateBy { it.itemId }
        val continueItems = progress.mapNotNull { entry ->
            val watched = historyById[entry.itemId] ?: return@mapNotNull null
            if (watched.type == MediaType.RADIO) return@mapNotNull null
            ContinueItem(watched.toPlayable(), entry.percent)
        }
        HomeUiState(
            continueWatching = continueItems,
            favorites = listTriple.first,
            recentlyAdded = listTriple.second,
            recentlyPlayed = listTriple.third,
            tvCount = countTriple.first,
            radioCount = countTriple.second,
            movieCount = countTriple.third
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
