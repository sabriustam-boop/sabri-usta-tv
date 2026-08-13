package com.sabriusta.tv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sabriusta.tv.data.local.MediaItemEntity

/** Canli TV ve Filmler bolumlerinin ortak liste duzeni. */
@Composable
fun BrowseContent(
    title: String,
    items: List<MediaItemEntity>,
    categories: List<String>,
    selectedCategory: String,
    query: String,
    favoriteIds: Set<String>,
    isLoading: Boolean,
    emptyTitle: String,
    emptyDescription: String,
    searchPlaceholder: String,
    onQueryChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onItemClick: (MediaItemEntity) -> Unit,
    onFavoriteClick: (MediaItemEntity) -> Unit,
    onEmptyAction: (() -> Unit)? = null,
    emptyActionLabel: String? = null,
    header: (@Composable () -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SectionHeader(title)
        header?.invoke()
        SearchField(value = query, onValueChange = onQueryChange, placeholder = searchPlaceholder)
        CategoryChips(
            categories = categories,
            selected = selectedCategory,
            onSelect = onCategorySelect
        )
        Spacer(Modifier.height(8.dp))
        when {
            isLoading -> LoadingBox()
            items.isEmpty() -> EmptyState(
                title = emptyTitle,
                description = emptyDescription,
                actionLabel = emptyActionLabel,
                onAction = onEmptyAction
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    MediaRowItem(
                        title = item.name,
                        subtitle = item.category,
                        logoUrl = item.logoUrl,
                        isFavorite = favoriteIds.contains(item.id),
                        onFavoriteClick = { onFavoriteClick(item) },
                        onClick = { onItemClick(item) }
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}
