package com.sabriusta.tv.ui.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.sabriusta.tv.core.NetworkMonitor
import com.sabriusta.tv.core.PlaybackErrors
import com.sabriusta.tv.core.UrlValidator
import com.sabriusta.tv.data.local.MediaType
import com.sabriusta.tv.data.prefs.SettingsRepository
import com.sabriusta.tv.data.repo.MediaRepository
import com.sabriusta.tv.data.repo.PlayableItem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class TrackOption(
    val label: String,
    val groupIndex: Int,
    val trackIndex: Int,
    val isSelected: Boolean
)

data class PlayerUiState(
    val item: PlayableItem? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isLive: Boolean = false,
    val isFavorite: Boolean = false,
    val playbackSpeed: Float = 1f,
    val videoTracks: List<TrackOption> = emptyList(),
    val audioTracks: List<TrackOption> = emptyList(),
    val textTracks: List<TrackOption> = emptyList(),
    val subtitlesEnabled: Boolean = true,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val infoMessage: String? = null
)

@OptIn(UnstableApi::class)
@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MediaRepository,
    private val settings: SettingsRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state

    private var queue: List<PlayableItem> = emptyList()
    private var queueIndex: Int = -1
    private var externalSubtitleUri: Uri? = null

    val player: ExoPlayer by lazy { buildPlayer() }

    private val listener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            _state.value = _state.value.copy(
                error = PlaybackErrors.fromMedia3ErrorCode(
                    error.errorCode,
                    networkMonitor.currentState().isOnline
                ),
                isLoading = false
            )
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _state.value = _state.value.copy(
                isLoading = playbackState == Player.STATE_BUFFERING,
                isLive = player.isCurrentMediaItemLive
            )
        }

        override fun onTracksChanged(tracks: Tracks) {
            updateTrackOptions(tracks)
        }
    }

    private fun buildPlayer(): ExoPlayer {
        val bufferSeconds = 30
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        val httpFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent("SabriUstaTV/1.0.0")
        val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                bufferSeconds * 1000 / 2,
                bufferSeconds * 1000,
                2_000,
                5_000
            )
            .build()
        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setLoadControl(loadControl)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
            .also { it.addListener(listener) }
    }

    fun load(itemId: String) {
        if (_state.value.item?.id == itemId) return
        viewModelScope.launch {
            val item = repository.findPlayable(itemId)
            if (item == null) {
                _state.value = _state.value.copy(
                    error = "Icerik bulunamadi. Liste silinmis olabilir.",
                    isLoading = false
                )
                return@launch
            }

            val gate = checkPlaybackAllowed(item)
            if (gate != null) {
                _state.value = _state.value.copy(item = item, error = gate, isLoading = false)
                return@launch
            }

            queue = repository.queueForType(item.type)
            queueIndex = queue.indexOfFirst { it.id == item.id }

            _state.value = _state.value.copy(
                item = item,
                error = null,
                isFavorite = repository.isFavorite(item.id),
                hasNext = queueIndex >= 0 && queueIndex < queue.size - 1,
                hasPrevious = queueIndex > 0,
                isLoading = true
            )
            startPlayback(item)
            repository.recordPlayback(item)
        }
    }

    private suspend fun checkPlaybackAllowed(item: PlayableItem): String? {
        if (item.isLocalFile) return null
        val network = networkMonitor.currentState()
        if (!network.isOnline) return PlaybackErrors.NO_INTERNET
        val current = settings.current()
        if (current.wifiOnly && !network.isUnmetered) return PlaybackErrors.WIFI_ONLY
        val validation = UrlValidator.validate(item.url, current.allowHttp)
        if (validation is UrlValidator.Result.Invalid) return validation.reason
        return null
    }

    private suspend fun startPlayback(item: PlayableItem) {
        val startPosition = if (item.type == MediaType.MOVIE) repository.progressOf(item.id) else 0L
        val builder = MediaItem.Builder()
            .setMediaId(item.id)
            .setUri(item.url)
        externalSubtitleUri?.let { uri ->
            builder.setSubtitleConfigurations(
                listOf(
                    MediaItem.SubtitleConfiguration.Builder(uri)
                        .setMimeType(guessSubtitleMime(uri))
                        .setLanguage("tr")
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build()
                )
            )
        }
        player.setMediaItem(builder.build(), startPosition)
        player.prepare()
        player.playWhenReady = true
    }

    private fun guessSubtitleMime(uri: Uri): String {
        val path = uri.toString().lowercase()
        return when {
            path.endsWith(".vtt") -> MimeTypes.TEXT_VTT
            path.endsWith(".ssa") || path.endsWith(".ass") -> MimeTypes.TEXT_SSA
            else -> MimeTypes.APPLICATION_SUBRIP
        }
    }

    fun retry() {
        val item = _state.value.item ?: return
        viewModelScope.launch {
            val gate = checkPlaybackAllowed(item)
            if (gate != null) {
                _state.value = _state.value.copy(error = gate)
                return@launch
            }
            _state.value = _state.value.copy(error = null, isLoading = true)
            startPlayback(item)
        }
    }

    fun playNext() {
        if (queueIndex < 0 || queueIndex >= queue.size - 1) return
        saveProgress()
        val next = queue[queueIndex + 1]
        _state.value = _state.value.copy(item = null)
        load(next.id)
    }

    fun playPrevious() {
        if (queueIndex <= 0) return
        saveProgress()
        val previous = queue[queueIndex - 1]
        _state.value = _state.value.copy(item = null)
        load(previous.id)
    }

    fun toggleFavorite() {
        val item = _state.value.item ?: return
        viewModelScope.launch {
            val isFav = repository.toggleFavorite(item)
            _state.value = _state.value.copy(isFavorite = isFav)
        }
    }

    fun setSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
        _state.value = _state.value.copy(playbackSpeed = speed)
    }

    fun goToLiveEdge() {
        if (player.isCurrentMediaItemLive) player.seekToDefaultPosition()
    }

    fun selectExternalSubtitle(uri: Uri) {
        externalSubtitleUri = uri
        val item = _state.value.item ?: return
        val position = player.currentPosition
        viewModelScope.launch {
            startPlayback(item)
            player.seekTo(position)
            _state.value = _state.value.copy(infoMessage = "Haricî altyazi yuklendi.")
        }
    }

    fun applyTrack(trackType: Int, option: TrackOption?) {
        val parameters = player.trackSelectionParameters.buildUpon()
        if (option == null) {
            parameters.clearOverridesOfType(trackType)
        } else {
            val group = player.currentTracks.groups.getOrNull(option.groupIndex)
            if (group != null) {
                parameters.setOverrideForType(
                    TrackSelectionOverride(group.mediaTrackGroup, option.trackIndex)
                )
            }
        }
        parameters.setTrackTypeDisabled(trackType, false)
        player.trackSelectionParameters = parameters.build()
    }

    fun setSubtitlesEnabled(enabled: Boolean) {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !enabled)
            .build()
        _state.value = _state.value.copy(subtitlesEnabled = enabled)
    }

    private fun updateTrackOptions(tracks: Tracks) {
        val video = mutableListOf<TrackOption>()
        val audio = mutableListOf<TrackOption>()
        val text = mutableListOf<TrackOption>()

        tracks.groups.forEachIndexed { groupIndex, group ->
            for (trackIndex in 0 until group.length) {
                if (!group.isTrackSupported(trackIndex)) continue
                val format = group.getTrackFormat(trackIndex)
                val selected = group.isTrackSelected(trackIndex)
                when (group.type) {
                    C.TRACK_TYPE_VIDEO -> {
                        val label = if (format.height > 0) {
                            "${format.height}p" + if (format.bitrate > 0) " (${format.bitrate / 1000} kbps)" else ""
                        } else {
                            "Video ${trackIndex + 1}"
                        }
                        video += TrackOption(label, groupIndex, trackIndex, selected)
                    }
                    C.TRACK_TYPE_AUDIO -> {
                        val label = buildString {
                            append(format.language?.uppercase() ?: "Ses ${trackIndex + 1}")
                            if (format.channelCount > 0) append(" • ${format.channelCount} kanal")
                        }
                        audio += TrackOption(label, groupIndex, trackIndex, selected)
                    }
                    C.TRACK_TYPE_TEXT -> {
                        val label = format.label ?: format.language?.uppercase() ?: "Altyazi ${trackIndex + 1}"
                        text += TrackOption(label, groupIndex, trackIndex, selected)
                    }
                }
            }
        }
        _state.value = _state.value.copy(
            videoTracks = video,
            audioTracks = audio,
            textTracks = text
        )
    }

    fun saveProgress() {
        val item = _state.value.item ?: return
        if (item.type != MediaType.MOVIE) return
        val position = player.currentPosition
        val duration = player.duration.takeIf { it > 0 } ?: 0L
        viewModelScope.launch { repository.saveProgress(item.id, position, duration) }
    }

    fun clearInfoMessage() {
        _state.value = _state.value.copy(infoMessage = null)
    }

    override fun onCleared() {
        saveProgress()
        player.removeListener(listener)
        player.release()
        super.onCleared()
    }
}
