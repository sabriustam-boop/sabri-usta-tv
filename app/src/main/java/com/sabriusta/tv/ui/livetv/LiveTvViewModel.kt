package com.sabriusta.tv.ui.livetv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sabriusta.tv.data.local.MediaItemEntity
import com.sabriusta.tv.data.local.MediaType
import com.sabriusta.tv.data.repo.MediaRepository
import com.sabriusta.tv.data.repo.toPlayable
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

data class BrowseUiState(
    val items: List<MediaItemEntity> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String = "",
    val query: String = "",
    val favoriteIds: Set<String> = emptySet(),
    val lastWatchedName: String? = null,
    val lastWatchedId: String? = null,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LiveTvViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val category = MutableStateFlow("")

    private val filtered = combine(query, category) { q, c -> q to c }
        .flatMapLatest { (q, c) -> repository.search(MediaType.TV, c, q) }

    val state: StateFlow<BrowseUiState> = combine(
        filtered,
        repository.observeCategories(MediaType.TV),
        repository.observeFavoriteIds(),
        repository.observeLastWatched(MediaType.TV),
        combine(query, category) { q, c -> q to c }
    ) { items, categories, favorites, lastWatched, filters ->
        BrowseUiState(
            items = items,
            categories = categories,
            selectedCategory = filters.second,
            query = filters.first,
            favoriteIds = favorites.toSet(),
            lastWatchedName = lastWatched?.name,
            lastWatchedId = lastWatched?.itemId,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BrowseUiState())

    fun onQueryChange(value: String) { query.value = value }
    fun onCategorySelect(value: String) { category.value = value }

    fun toggleFavorite(item: MediaItemEntity) {
        viewModelScope.launch { repository.toggleFavorite(item.toPlayable()) }
    }
}
