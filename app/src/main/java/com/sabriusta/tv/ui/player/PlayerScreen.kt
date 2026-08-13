package com.sabriusta.tv.ui.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.sabriusta.tv.LocalPipController
import com.sabriusta.tv.ui.components.ErrorBox
import com.sabriusta.tv.ui.theme.Altin
import kotlinx.coroutines.delay

private val SPEED_OPTIONS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    itemId: String,
    autoFullscreen: Boolean,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context.findActivity()
    val pipController = LocalPipController.current

    var isLocked by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showTrackDialog by remember { mutableStateOf(false) }
    var overlayMessage by remember { mutableStateOf<String?>(null) }

    val subtitlePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> if (uri != null) viewModel.selectExternalSubtitle(uri) }

    LaunchedEffect(itemId) { viewModel.load(itemId) }

    // Ilerleme duzenli araliklarla kaydedilir; uygulama kapansa da kaldigi yer korunur.
    LaunchedEffect(state.item?.id) {
        while (true) {
            delay(5_000)
            viewModel.saveProgress()
        }
    }

    LaunchedEffect(autoFullscreen, state.item?.id) {
        if (autoFullscreen && state.item != null && activity != null) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.saveProgress()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_USER
            val window = activity?.window
            if (window != null) {
                val attributes = window.attributes
                attributes.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = attributes
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = viewModel.player
                    useController = true
                    controllerShowTimeoutMs = 4000
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            update = { view -> view.useController = !isLocked }
        )

        if (!isLocked) {
            // Sol kenar: parlaklik, sag kenar: ses seviyesi
            BrightnessStrip(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(88.dp),
                activity = activity,
                onMessage = { overlayMessage = it }
            )
            VolumeStrip(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(88.dp),
                context = context,
                onMessage = { overlayMessage = it }
            )

            TopControls(
                modifier = Modifier.align(Alignment.TopCenter),
                title = state.item?.name.orEmpty(),
                isFavorite = state.isFavorite,
                isLive = state.isLive,
                onBack = onBack,
                onFavorite = viewModel::toggleFavorite,
                onLock = { isLocked = true },
                onPip = { pipController.enterPip() },
                onRotate = {
                    activity?.let {
                        it.requestedOrientation =
                            if (it.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            } else {
                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            }
                    }
                },
                onLiveEdge = viewModel::goToLiveEdge
            )

            BottomControls(
                modifier = Modifier.align(Alignment.BottomCenter),
                hasNext = state.hasNext,
                hasPrevious = state.hasPrevious,
                onNext = viewModel::playNext,
                onPrevious = viewModel::playPrevious,
                onSpeed = { showSpeedDialog = true },
                onTracks = { showTrackDialog = true },
                onSubtitleFile = { subtitlePicker.launch(arrayOf("application/x-subrip", "text/vtt", "text/plain", "*/*")) }
            )
        } else {
            IconButton(
                onClick = { isLocked = false },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Filled.LockOpen, contentDescription = "Ekran kilidini ac", tint = Altin)
            }
        }

        state.error?.let { message ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color(0xCC000000))
            ) {
                ErrorBox(message = message, onRetry = viewModel::retry)
            }
        }

        overlayMessage?.let { message ->
            Text(
                text = message,
                color = Altin,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color(0xAA000000))
                    .padding(16.dp)
            )
            LaunchedEffect(message) {
                delay(900)
                overlayMessage = null
            }
        }

        state.infoMessage?.let { message ->
            Text(
                text = message,
                color = Altin,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
            )
            LaunchedEffect(message) {
                delay(2000)
                viewModel.clearInfoMessage()
            }
        }
    }

    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Oynatma hizi") },
            text = {
                Column {
                    SPEED_OPTIONS.forEach { speed ->
                        TextButton(onClick = {
                            viewModel.setSpeed(speed)
                            showSpeedDialog = false
                        }) {
                            Text(
                                text = if (speed == state.playbackSpeed) "${speed}x  ✓" else "${speed}x",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) { Text("Kapat") }
            }
        )
    }

    if (showTrackDialog) {
        TrackDialog(
            state = state,
            onDismiss = { showTrackDialog = false },
            onSelectVideo = { viewModel.applyTrack(C.TRACK_TYPE_VIDEO, it) },
            onSelectAudio = { viewModel.applyTrack(C.TRACK_TYPE_AUDIO, it) },
            onSelectText = { viewModel.applyTrack(C.TRACK_TYPE_TEXT, it) },
            onToggleSubtitles = viewModel::setSubtitlesEnabled
        )
    }
}

@Composable
private fun TopControls(
    modifier: Modifier,
    title: String,
    isFavorite: Boolean,
    isLive: Boolean,
    onBack: () -> Unit,
    onFavorite: () -> Unit,
    onLock: () -> Unit,
    onPip: () -> Unit,
    onRotate: () -> Unit,
    onLiveEdge: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0x99000000))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Geri", tint = Color.White)
        }
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        if (isLive) {
            TextButton(onClick = onLiveEdge) { Text("CANLI", color = Altin) }
        }
        IconButton(onClick = onFavorite) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = "Favori",
                tint = Altin
            )
        }
        IconButton(onClick = onRotate) {
            Icon(Icons.Filled.ScreenRotation, contentDescription = "Ekran yonu", tint = Color.White)
        }
        IconButton(onClick = onPip) {
            Icon(Icons.Filled.PictureInPicture, contentDescription = "Resim icinde resim", tint = Color.White)
        }
        IconButton(onClick = onLock) {
            Icon(Icons.Filled.Lock, contentDescription = "Ekrani kilitle", tint = Color.White)
        }
    }
}

@Composable
private fun BottomControls(
    modifier: Modifier,
    hasNext: Boolean,
    hasPrevious: Boolean,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSpeed: () -> Unit,
    onTracks: () -> Unit,
    onSubtitleFile: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0x99000000))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious, enabled = hasPrevious) {
            Icon(
                Icons.Filled.SkipPrevious,
                contentDescription = "Onceki yayin",
                tint = if (hasPrevious) Color.White else Color.Gray
            )
        }
        IconButton(onClick = onTracks) {
            Icon(Icons.Filled.Tune, contentDescription = "Kalite ve ses kanali", tint = Color.White)
        }
        IconButton(onClick = onSubtitleFile) {
            Icon(Icons.Filled.ClosedCaption, contentDescription = "Altyazi dosyasi sec", tint = Color.White)
        }
        IconButton(onClick = onSpeed) {
            Icon(Icons.Filled.Speed, contentDescription = "Oynatma hizi", tint = Color.White)
        }
        IconButton(onClick = onNext, enabled = hasNext) {
            Icon(
                Icons.Filled.SkipNext,
                contentDescription = "Sonraki yayin",
                tint = if (hasNext) Color.White else Color.Gray
            )
        }
    }
}

@Composable
private fun TrackDialog(
    state: PlayerUiState,
    onDismiss: () -> Unit,
    onSelectVideo: (TrackOption?) -> Unit,
    onSelectAudio: (TrackOption?) -> Unit,
    onSelectText: (TrackOption?) -> Unit,
    onToggleSubtitles: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yayin ayarlari") },
        text = {
            LazyColumn {
                item { Text("Goruntu kalitesi", style = MaterialTheme.typography.titleMedium) }
                item {
                    TextButton(onClick = { onSelectVideo(null) }) { Text("Otomatik") }
                }
                items(state.videoTracks) { option ->
                    TextButton(onClick = { onSelectVideo(option) }) {
                        Text(if (option.isSelected) "${option.label}  ✓" else option.label)
                    }
                }
                item { Text("Ses kanali", style = MaterialTheme.typography.titleMedium) }
                if (state.audioTracks.isEmpty()) {
                    item { Text("Ek ses kanali yok", style = MaterialTheme.typography.bodyMedium) }
                }
                items(state.audioTracks) { option ->
                    TextButton(onClick = { onSelectAudio(option) }) {
                        Text(if (option.isSelected) "${option.label}  ✓" else option.label)
                    }
                }
                item { Text("Altyazi", style = MaterialTheme.typography.titleMedium) }
                item {
                    TextButton(onClick = { onToggleSubtitles(!state.subtitlesEnabled) }) {
                        Text(if (state.subtitlesEnabled) "Altyaziyi kapat" else "Altyaziyi ac")
                    }
                }
                if (state.textTracks.isEmpty()) {
                    item { Text("Yayinda altyazi bulunamadi", style = MaterialTheme.typography.bodyMedium) }
                }
                items(state.textTracks) { option ->
                    TextButton(onClick = { onSelectText(option) }) {
                        Text(if (option.isSelected) "${option.label}  ✓" else option.label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Kapat") } }
    )
}

@Composable
private fun BrightnessStrip(
    modifier: Modifier,
    activity: Activity?,
    onMessage: (String) -> Unit
) {
    Box(
        modifier = modifier.pointerInput(activity) {
            detectVerticalDragGestures { _, dragAmount ->
                val window = activity?.window ?: return@detectVerticalDragGestures
                val attributes = window.attributes
                val current = if (attributes.screenBrightness < 0f) 0.5f else attributes.screenBrightness
                val updated = (current - dragAmount / 800f).coerceIn(0.01f, 1f)
                attributes.screenBrightness = updated
                window.attributes = attributes
                onMessage("Parlaklik %${(updated * 100).toInt()}")
            }
        }
    )
}

@Composable
private fun VolumeStrip(
    modifier: Modifier,
    context: Context,
    onMessage: (String) -> Unit
) {
    val audioManager = remember { context.getSystemService(AudioManager::class.java) }
    Box(
        modifier = modifier.pointerInput(audioManager) {
            detectVerticalDragGestures { _, dragAmount ->
                val manager = audioManager ?: return@detectVerticalDragGestures
                val max = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val current = manager.getStreamVolume(AudioManager.STREAM_MUSIC)
                val step = if (dragAmount < -6f) 1 else if (dragAmount > 6f) -1 else 0
                if (step != 0) {
                    val updated = (current + step).coerceIn(0, max)
                    manager.setStreamVolume(AudioManager.STREAM_MUSIC, updated, 0)
                    onMessage("Ses ${updated}/$max")
                }
            }
        }
    )
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is android.content.ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
