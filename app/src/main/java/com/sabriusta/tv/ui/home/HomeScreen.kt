package com.sabriusta.tv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sabriusta.tv.ui.components.EmptyState
import com.sabriusta.tv.ui.components.MediaCard
import com.sabriusta.tv.ui.components.SectionHeader
import com.sabriusta.tv.ui.nav.Routes
import com.sabriusta.tv.ui.theme.Altin
import com.sabriusta.tv.ui.theme.KoyuLacivert
import com.sabriusta.tv.ui.theme.Siyah

@Composable
fun HomeScreen(
    onOpenPlayer: (String) -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { HomeHeader() }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickTile(
                    title = "Canli TV",
                    subtitle = "${state.tvCount} kanal",
                    icon = Icons.Filled.LiveTv,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Routes.LIVE_TV) }
                )
                QuickTile(
                    title = "Radyo",
                    subtitle = "${state.radioCount} istasyon",
                    icon = Icons.Filled.Radio,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Routes.RADIO) }
                )
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickTile(
                    title = "Ucretsiz Filmler",
                    subtitle = "${state.movieCount} icerik",
                    icon = Icons.Filled.Movie,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Routes.MOVIES) }
                )
                QuickTile(
                    title = "M3U Listesi Ekle",
                    subtitle = "Baglanti veya dosya",
                    icon = Icons.Filled.Add,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Routes.PLAYLISTS) }
                )
            }
        }

        if (state.continueWatching.isNotEmpty()) {
            item { SectionHeader("Izlemeye devam et") }
            item {
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.continueWatching, key = { it.item.id }) { entry ->
                        MediaCard(
                            title = entry.item.name,
                            subtitle = "%${entry.percent} izlendi",
                            logoUrl = entry.item.logoUrl,
                            progressPercent = entry.percent,
                            onClick = { onOpenPlayer(entry.item.id) }
                        )
                    }
                }
            }
        }

        if (state.favorites.isNotEmpty()) {
            item { SectionHeader("Favori kanallar") }
            item {
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.favorites, key = { it.id }) { item ->
                        MediaCard(
                            title = item.name,
                            subtitle = item.category,
                            logoUrl = item.logoUrl,
                            onClick = { onOpenPlayer(item.id) }
                        )
                    }
                }
            }
        }

        if (state.recentlyPlayed.isNotEmpty()) {
            item { SectionHeader("Son oynatilanlar") }
            item {
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.recentlyPlayed, key = { it.id }) { item ->
                        MediaCard(
                            title = item.name,
                            subtitle = turkishType(item.type),
                            logoUrl = item.logoUrl,
                            onClick = { onOpenPlayer(item.id) }
                        )
                    }
                }
            }
        }

        if (state.recentlyAdded.isNotEmpty()) {
            item { SectionHeader("Son eklenen yayinlar") }
            item {
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.recentlyAdded, key = { it.id }) { item ->
                        MediaCard(
                            title = item.name,
                            subtitle = item.category,
                            logoUrl = item.logoUrl,
                            onClick = { onOpenPlayer(item.id) }
                        )
                    }
                }
            }
        }

        if (state.tvCount == 0 && state.radioCount == 0 && state.movieCount == 0) {
            item {
                EmptyState(
                    title = "Henuz yayin yok",
                    description = "Kendi M3U listenizi ekleyerek kanallarinizi ve radyolarinizi getirin.",
                    actionLabel = "M3U listesi ekle",
                    onAction = { onNavigate(Routes.PLAYLISTS) }
                )
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun HomeHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(KoyuLacivert, Siyah)))
            .padding(horizontal = 20.dp, vertical = 28.dp)
    ) {
        Column {
            Text(
                text = "Sabri Usta TV",
                style = MaterialTheme.typography.displaySmall,
                color = Altin,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "TV • Radyo • Film",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Tek Uygulamada Keyifli Yayin",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(112.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, contentDescription = null, tint = Altin, modifier = Modifier.size(30.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun turkishType(type: String): String = when (type) {
    "RADIO" -> "Radyo"
    "MOVIE" -> "Film"
    else -> "TV"
}
