package com.sabriusta.tv.ui.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sabriusta.tv.data.local.MediaItemEntity
import com.sabriusta.tv.data.local.MediaType
import com.sabriusta.tv.data.repo.MediaRepository
import com.sabriusta.tv.data.repo.toPlayable
import com.sabriusta.tv.ui.livetv.BrowseUiState
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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MoviesViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val category = MutableStateFlow("")

    private val filtered = combine(query, category) { q, c -> q to c }
        .flatMapLatest { (q, c) -> repository.search(MediaType.MOVIE, c, q) }

    val state: StateFlow<BrowseUiState> = combine(
        filtered,
        repository.observeCategories(MediaType.MOVIE),
        repository.observeFavoriteIds(),
        combine(query, category) { q, c -> q to c }
    ) { items, categories, favorites, filters ->
        BrowseUiState(
            items = items,
            categories = categories,
            selectedCategory = filters.second,
            query = filters.first,
            favoriteIds = favorites.toSet(),
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BrowseUiState())

    fun onQueryChange(value: String) { query.value = value }
    fun onCategorySelect(value: String) { category.value = value }

    fun toggleFavorite(item: MediaItemEntity) {
        viewModelScope.launch { repository.toggleFavorite(item.toPlayable()) }
    }

    /** Cihazdan secilen yerel video dosyasini kutuphaneye ekler. */
    fun addLocalVideo(name: String, uri: String, onAdded: (String) -> Unit) {
        viewModelScope.launch {
            val entity = repository.addCustomSource(name, uri, MediaType.MOVIE, isLocalFile = true)
            onAdded(entity.id)
        }
    }
}
