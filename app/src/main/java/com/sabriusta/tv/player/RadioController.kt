package com.sabriusta.tv.player

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.sabriusta.tv.core.PlaybackErrors
import com.sabriusta.tv.core.NetworkMonitor
import com.sabriusta.tv.data.repo.PlayableItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.guava.await
import javax.inject.Inject
import javax.inject.Singleton

data class RadioState(
    val currentId: String? = null,
    val currentName: String? = null,
    val currentLogo: String? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val error: String? = null,
    val sleepTimerMinutes: Int = 0
)

/**
 * Arayuz ile RadioPlaybackService arasindaki tek baglanti noktasi.
 */
@UnstableApi
@Singleton
class RadioController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val networkMonitor: NetworkMonitor
) {
    private val _state = MutableStateFlow(RadioState())
    val state: StateFlow<RadioState> = _state

    private var controller: MediaController? = null

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.value = _state.value.copy(isPlaying = isPlaying)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _state.value = _state.value.copy(isBuffering = playbackState == Player.STATE_BUFFERING)
        }

        override fun onPlayerError(error: PlaybackException) {
            _state.value = _state.value.copy(
                error = PlaybackErrors.fromMedia3ErrorCode(
                    error.errorCode,
                    networkMonitor.currentState().isOnline
                ),
                isPlaying = false,
                isBuffering = false
            )
        }
    }

    private suspend fun requireController(): MediaController {
        controller?.let { if (it.isConnected) return it }
        val token = SessionToken(context, ComponentName(context, RadioPlaybackService::class.java))
        val newController = MediaController.Builder(context, token).buildAsync().await()
        newController.addListener(listener)
        controller = newController
        return newController
    }

    suspend fun play(item: PlayableItem) {
        val mediaController = requireController()
        val mediaItem = MediaItem.Builder()
            .setMediaId(item.id)
            .setUri(item.url)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(item.name)
                    .setArtist(item.category)
                    .setArtworkUri(item.logoUrl?.let { android.net.Uri.parse(it) })
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build()
            )
            .build()
        _state.value = _state.value.copy(
            currentId = item.id,
            currentName = item.name,
            currentLogo = item.logoUrl,
            error = null,
            isBuffering = true
        )
        mediaController.setMediaItem(mediaItem)
        mediaController.prepare()
        mediaController.play()
    }

    suspend fun togglePlayPause() {
        val mediaController = requireController()
        if (mediaController.isPlaying) mediaController.pause() else mediaController.play()
    }

    suspend fun stop() {
        val mediaController = requireController()
        mediaController.stop()
        mediaController.clearMediaItems()
        _state.value = RadioState()
    }

    suspend fun setSleepTimer(minutes: Int) {
        val mediaController = requireController()
        val args = Bundle().apply { putInt(RadioPlaybackService.EXTRA_SLEEP_MINUTES, minutes) }
        mediaController.sendCustomCommand(
            SessionCommand(RadioPlaybackService.COMMAND_SLEEP_TIMER, Bundle.EMPTY),
            args
        )
        _state.value = _state.value.copy(sleepTimerMinutes = minutes)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun release() {
        controller?.removeListener(listener)
        controller?.release()
        controller = null
    }
}
