package com.sabriusta.tv.ui.movies

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sabriusta.tv.ui.components.BrowseContent

@Composable
fun MoviesScreen(
    onOpenPlayer: (String) -> Unit,
    viewModel: MoviesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Storage Access Framework - genis depolama izni istenmez.
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val name = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')
                ?: "Cihazdaki video"
            viewModel.addLocalVideo(name, uri.toString()) { id -> onOpenPlayer(id) }
        }
    }

    BrowseContent(
        title = "Filmler",
        items = state.items,
        categories = state.categories,
        selectedCategory = state.selectedCategory,
        query = state.query,
        favoriteIds = state.favoriteIds,
        isLoading = state.isLoading,
        emptyTitle = "Film bulunamadi",
        emptyDescription = "Kamu mali filmler ornek katalogda gelir. Kendi yasal film baglantilarinizi M3U listesi olarak ekleyebilir veya cihazinizdaki bir video dosyasini secebilirsiniz.",
        searchPlaceholder = "Film ara",
        onQueryChange = viewModel::onQueryChange,
        onCategorySelect = viewModel::onCategorySelect,
        onItemClick = { onOpenPlayer(it.id) },
        onFavoriteClick = viewModel::toggleFavorite,
        header = {
            AssistChip(
                onClick = { picker.launch(arrayOf("video/*")) },
                label = {
                    Text(
                        "Cihazdan video sec",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }
    )
}
