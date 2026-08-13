package com.sabriusta.tv.ui.playlists

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sabriusta.tv.ui.components.ErrorBox
import com.sabriusta.tv.ui.theme.Altin
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlaylistsScreen(
    onBack: () -> Unit,
    viewModel: PlaylistsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var pastedText by remember { mutableStateOf("") }
    var autoUpdate by remember { mutableStateOf(false) }
    var showPasteField by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) viewModel.addFromUri(name.ifBlank { "Dosyadan liste" }, uri)
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Geri")
                }
                Text("M3U Listeleri", style = MaterialTheme.typography.headlineSmall)
            }
        }

        if (state.isBusy) {
            item {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Altin
                )
            }
        }

        state.message?.let { message ->
            item {
                if (state.isError) {
                    ErrorBox(message = message, onRetry = viewModel::clearMessage)
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(message, style = MaterialTheme.typography.bodyLarge)
                            TextButton(onClick = viewModel::clearMessage) { Text("Tamam") }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Yeni liste ekle", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Liste adi") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("M3U baglantisi (https://...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Otomatik guncelle", style = MaterialTheme.typography.bodyLarge)
                        Switch(checked = autoUpdate, onCheckedChange = { autoUpdate = it })
                    }
                    Button(
                        onClick = {
                            viewModel.addFromUrl(name, url, autoUpdate)
                            url = ""
                        },
                        enabled = url.isNotBlank() && !state.isBusy,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Baglantidan ekle") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { filePicker.launch(arrayOf("*/*")) },
                        enabled = !state.isBusy,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Telefondan M3U dosyasi sec") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showPasteField = !showPasteField },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (showPasteField) "Metin alanini gizle" else "Metni dogrudan yapistir") }

                    if (showPasteField) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = pastedText,
                            onValueChange = { pastedText = it },
                            label = { Text("#EXTM3U ile baslayan liste metni") },
                            minLines = 4,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                viewModel.addFromText(name, pastedText)
                                pastedText = ""
                            },
                            enabled = pastedText.isNotBlank() && !state.isBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Metinden ekle") }
                    }
                }
            }
        }

        items(state.playlists, key = { it.id }) { playlist ->
            PlaylistCard(
                playlist = playlist,
                onRefresh = { viewModel.refresh(playlist.id) },
                onToggleEnabled = { viewModel.setEnabled(playlist.id, it) },
                onToggleAutoUpdate = { viewModel.setAutoUpdate(playlist.id, it) },
                onRename = { viewModel.rename(playlist.id, it) },
                onDelete = { viewModel.delete(playlist.id) }
            )
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun PlaylistCard(
    playlist: com.sabriusta.tv.data.local.PlaylistEntity,
    onRefresh: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleAutoUpdate: (Boolean) -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var newName by remember(playlist.id) { mutableStateOf(playlist.name) }
    var confirmDelete by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            if (editing) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Liste adi") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row {
                    TextButton(onClick = {
                        onRename(newName)
                        editing = false
                    }) { Text("Kaydet") }
                    TextButton(onClick = { editing = false }) { Text("Vazgec") }
                }
            } else {
                Text(playlist.name, style = MaterialTheme.typography.titleMedium)
            }

            Text(
                text = "${playlist.itemCount} yayin • ${sourceLabel(playlist.sourceType)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Son guncelleme: ${formatDate(playlist.lastUpdatedAt)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            playlist.lastError?.let { error ->
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Uyari: $error",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Etkin", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = playlist.enabled, onCheckedChange = onToggleEnabled)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Otomatik guncelle", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = playlist.autoUpdate,
                    onCheckedChange = onToggleAutoUpdate,
                    enabled = playlist.sourceType == com.sabriusta.tv.data.local.SourceType.URL
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onRefresh) { Text("Yenile") }
                TextButton(onClick = { editing = true }) { Text("Duzenle") }
                if (!playlist.isBuiltIn) {
                    TextButton(onClick = { confirmDelete = true }) { Text("Sil") }
                }
            }

            if (confirmDelete) {
                Text(
                    "Bu liste ve icindeki yayinlar silinecek. Emin misiniz?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Row {
                    TextButton(onClick = {
                        confirmDelete = false
                        onDelete()
                    }) { Text("Evet, sil") }
                    TextButton(onClick = { confirmDelete = false }) { Text("Vazgec") }
                }
            }
        }
    }
}

private fun sourceLabel(sourceType: String): String = when (sourceType) {
    com.sabriusta.tv.data.local.SourceType.URL -> "Baglanti"
    com.sabriusta.tv.data.local.SourceType.FILE -> "Dosya"
    com.sabriusta.tv.data.local.SourceType.TEXT -> "Yapistirilan metin"
    com.sabriusta.tv.data.local.SourceType.CATALOG -> "Ornek katalog"
    else -> "Diger"
}

private fun formatDate(time: Long): String {
    if (time <= 0) return "henuz guncellenmedi"
    val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR"))
    return formatter.format(Date(time))
}
