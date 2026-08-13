package com.sabriusta.tv.ui.radio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sabriusta.tv.ui.components.CategoryChips
import com.sabriusta.tv.ui.components.EmptyState
import com.sabriusta.tv.ui.components.ErrorBox
import com.sabriusta.tv.ui.components.LoadingBox
import com.sabriusta.tv.ui.components.MediaRowItem
import com.sabriusta.tv.ui.components.SearchField
import com.sabriusta.tv.ui.components.SectionHeader
import com.sabriusta.tv.ui.theme.Altin

private val SLEEP_OPTIONS = listOf(15, 30, 45, 60, 90)

@Composable
fun RadioScreen(viewModel: RadioViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        SectionHeader("Radyo")

        state.player.currentName?.let { name ->
            NowPlayingBar(
                name = name,
                isPlaying = state.player.isPlaying,
                isBuffering = state.player.isBuffering,
                onToggle = viewModel::togglePlayPause,
                onStop = viewModel::stop
            )
            SleepTimerRow(
                selected = state.player.sleepTimerMinutes,
                onSelect = viewModel::setSleepTimer
            )
        }

        val message = state.blockedMessage ?: state.player.error
        if (message != null) {
            ErrorBox(message = message, onRetry = viewModel::clearMessages)
        }

        val lastId = state.lastPlayedId
        val lastName = state.lastPlayedName
        if (lastId != null && lastName != null && state.player.currentId == null) {
            AssistChip(
                onClick = { viewModel.playById(lastId) },
                label = { Text("Son calinan: $lastName", style = MaterialTheme.typography.bodyMedium) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }

        SearchField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            placeholder = "Istasyon ara"
        )
        CategoryChips(
            categories = state.categories,
            selected = state.selectedCategory,
            onSelect = viewModel::onCategorySelect
        )
        Spacer(Modifier.height(8.dp))

        when {
            state.isLoading -> LoadingBox()
            state.items.isEmpty() -> EmptyState(
                title = "Istasyon bulunamadi",
                description = "Radyo istasyonlari icin M3U listenizi ekleyin veya ornek katalogdaki istasyonlari deneyin."
            )
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.items, key = { it.id }) { item ->
                    MediaRowItem(
                        title = item.name,
                        subtitle = item.category,
                        logoUrl = item.logoUrl,
                        isFavorite = state.favoriteIds.contains(item.id),
                        onFavoriteClick = { viewModel.toggleFavorite(item) },
                        onClick = { viewModel.play(item) }
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun NowPlayingBar(
    name: String,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onToggle: () -> Unit,
    onStop: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when {
                        isBuffering -> "Baglaniliyor..."
                        isPlaying -> "Caliyor - arka planda devam eder"
                        else -> "Duraklatildi"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Duraklat" else "Oynat",
                    tint = Altin
                )
            }
            IconButton(onClick = onStop) {
                Icon(Icons.Filled.Stop, contentDescription = "Durdur", tint = Altin)
            }
        }
    }
}

@Composable
private fun SleepTimerRow(selected: Int, onSelect: (Int) -> Unit) {
    Column {
        Text(
            text = "Uyku zamanlayicisi",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(listOf(0) + SLEEP_OPTIONS) { minutes ->
                FilterChip(
                    selected = selected == minutes,
                    onClick = { onSelect(minutes) },
                    label = {
                        Text(if (minutes == 0) "Kapali" else "$minutes dk")
                    }
                )
            }
        }
    }
}
