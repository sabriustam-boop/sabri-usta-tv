package com.sabriusta.tv.ui.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sabriusta.tv.data.prefs.OrientationMode
import com.sabriusta.tv.data.prefs.ThemeMode
import com.sabriusta.tv.ui.components.SectionHeader

@Composable
fun SettingsScreen(
    onOpenPlaylists: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings = state.settings

    val exportFavorites = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? -> if (uri != null) viewModel.exportFavorites(uri) }

    val importFavorites = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> if (uri != null) viewModel.importFavorites(uri) }

    val exportPlaylists = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? -> if (uri != null) viewModel.exportPlaylists(uri) }

    val importPlaylists = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> if (uri != null) viewModel.importPlaylists(uri) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { SectionHeader("Ayarlar") }

        state.message?.let { message ->
            item {
                SettingsCard {
                    Text(message, style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = viewModel::clearMessage) { Text("Tamam") }
                }
            }
        }

        item {
            SettingsCard {
                Text("Tema", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { viewModel.setTheme(mode) },
                            label = { Text(themeLabel(mode)) }
                        )
                    }
                }
            }
        }

        item {
            SettingsCard {
                Text("Varsayilan ekran yonu", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OrientationMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.orientationMode == mode,
                            onClick = { viewModel.setOrientation(mode) },
                            label = { Text(orientationLabel(mode)) }
                        )
                    }
                }
                SwitchRow(
                    title = "Oynatirken otomatik tam ekran",
                    checked = settings.autoFullscreen,
                    onCheckedChange = viewModel::setAutoFullscreen
                )
            }
        }

        item {
            SettingsCard {
                Text("Baglanti", style = MaterialTheme.typography.titleMedium)
                SwitchRow(
                    title = "Yalnizca Wi-Fi uzerinden oynat",
                    checked = settings.wifiOnly,
                    onCheckedChange = viewModel::setWifiOnly
                )
                SwitchRow(
                    title = "Mobil veri uyarisi goster",
                    checked = settings.mobileDataWarning,
                    onCheckedChange = viewModel::setMobileWarning
                )
                SwitchRow(
                    title = "Ayni yayinlari tekilleştir",
                    checked = settings.deduplicate,
                    onCheckedChange = viewModel::setDeduplicate
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Oynatici tampon boyutu: ${settings.bufferSeconds} saniye",
                    style = MaterialTheme.typography.bodyLarge
                )
                Slider(
                    value = settings.bufferSeconds.toFloat(),
                    onValueChange = { viewModel.setBuffer(it.toInt()) },
                    valueRange = 10f..120f,
                    steps = 10
                )
            }
        }

        item {
            SettingsCard {
                Text("Guvenlik", style = MaterialTheme.typography.titleMedium)
                SwitchRow(
                    title = "HTTP yayinlarina izin ver",
                    checked = settings.allowHttp,
                    onCheckedChange = viewModel::setAllowHttp
                )
                Text(
                    "Risk uyarisi: HTTP baglantilari sifresizdir. Ag uzerindeki ucuncu kisiler ne izlediginizi " +
                        "gorebilir ve yayini degistirebilir. Varsayilan olarak kapalidir; yalnizca guvendiginiz " +
                        "kaynaklar icin acin.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        item {
            SettingsCard {
                Text("Listeler ve veriler", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = onOpenPlaylists, modifier = Modifier.fillMaxWidth()) {
                    Text("M3U listelerini yonet")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { exportFavorites.launch("sabri-usta-tv-favoriler.json") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Favorileri disa aktar") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { importFavorites.launch(arrayOf("application/json", "*/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Favorileri ice aktar") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { exportPlaylists.launch("sabri-usta-tv-listeler.json") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("M3U listelerini disa aktar") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { importPlaylists.launch(arrayOf("application/json", "*/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("M3U listelerini ice aktar") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = viewModel::clearHistory, modifier = Modifier.fillMaxWidth()) {
                    Text("Izleme gecmisini temizle")
                }
            }
        }

        item {
            SettingsCard {
                Text("Uygulama hakkinda", style = MaterialTheme.typography.titleMedium)
                Text("Sabri Usta TV surum 1.0.0", style = MaterialTheme.typography.bodyLarge)
                Text("TV • Radyo • Film", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Tek Uygulamada Keyifli Yayin",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            SettingsCard {
                Text("Gizlilik", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Uygulama reklam agi veya takip sistemi icermez. Favoriler, gecmis ve M3U listeleriniz " +
                        "yalnizca cihazinizda saklanir; hicbir sunucuya gonderilmez. Yayin adresleriniz " +
                        "kayitlarda maskelenir ve cihaz yedeklemesinin disinda tutulur.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        item {
            SettingsCard {
                Text("Yasal kullanim bildirimi", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Sabri Usta TV yalnizca resmi, ucretsiz, kamu mali veya kullanicinin erisim hakkina sahip " +
                        "oldugu yayinlari oynatmak amaciyla gelistirilmistir. Kullanici ekledigi yayin " +
                        "kaynaklarinin kullanim hakkindan sorumludur.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> "Sistem"
    ThemeMode.LIGHT -> "Acik"
    ThemeMode.DARK -> "Koyu"
}

private fun orientationLabel(mode: OrientationMode): String = when (mode) {
    OrientationMode.AUTO -> "Otomatik"
    OrientationMode.PORTRAIT -> "Dikey"
    OrientationMode.LANDSCAPE -> "Yatay"
}
