package com.sabriusta.tv.ui.livetv

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sabriusta.tv.ui.components.BrowseContent

@Composable
fun LiveTvScreen(
    onOpenPlayer: (String) -> Unit,
    viewModel: LiveTvViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BrowseContent(
        title = "Canli TV",
        items = state.items,
        categories = state.categories,
        selectedCategory = state.selectedCategory,
        query = state.query,
        favoriteIds = state.favoriteIds,
        isLoading = state.isLoading,
        emptyTitle = "Kanal bulunamadi",
        emptyDescription = "Arama veya kategori filtresini degistirin ya da Ayarlar > M3U listeleri bolumunden yeni liste ekleyin.",
        searchPlaceholder = "Kanal ara",
        onQueryChange = viewModel::onQueryChange,
        onCategorySelect = viewModel::onCategorySelect,
        onItemClick = { onOpenPlayer(it.id) },
        onFavoriteClick = viewModel::toggleFavorite,
        header = {
            val lastId = state.lastWatchedId
            val lastName = state.lastWatchedName
            if (lastId != null && lastName != null) {
                AssistChip(
                    onClick = { onOpenPlayer(lastId) },
                    label = { Text("Son izlenen: $lastName", style = MaterialTheme.typography.bodyMedium) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
        }
    )
}
