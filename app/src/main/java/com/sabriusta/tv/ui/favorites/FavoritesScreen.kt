package com.sabriusta.tv.ui.favorites

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sabriusta.tv.ui.components.EmptyState
import com.sabriusta.tv.ui.components.LoadingBox
import com.sabriusta.tv.ui.components.MediaRowItem
import com.sabriusta.tv.ui.components.SectionHeader
import com.sabriusta.tv.ui.home.turkishType

@Composable
fun FavoritesScreen(
    onOpenPlayer: (String) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        SectionHeader("Favoriler")
        when {
            state.isLoading -> LoadingBox()
            state.favorites.isEmpty() -> EmptyState(
                title = "Favori yok",
                description = "Kanal, istasyon veya film kartlarindaki kalp simgesine dokunarak favori ekleyebilirsiniz."
            )
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.favorites, key = { it.itemId }) { item ->
                    MediaRowItem(
                        title = item.name,
                        subtitle = "${turkishType(item.type)} • ${item.category}",
                        logoUrl = item.logoUrl,
                        isFavorite = true,
                        onFavoriteClick = { viewModel.remove(item) },
                        onClick = {
                            if (viewModel.isRadio(item)) {
                                viewModel.playRadio(item)
                            } else {
                                onOpenPlayer(item.itemId)
                            }
                        }
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}
