package com.sabriusta.tv.ui.playlists

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sabriusta.tv.data.local.PlaylistEntity
import com.sabriusta.tv.data.repo.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistsUiState(
    val playlists: List<PlaylistEntity> = emptyList(),
    val isBusy: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false
)

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val repository: PlaylistRepository
) : ViewModel() {

    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<Pair<String, Boolean>?>(null)

    val state: StateFlow<PlaylistsUiState> = combine(
        repository.observePlaylists(),
        busy,
        message
    ) { playlists, isBusy, msg ->
        PlaylistsUiState(
            playlists = playlists,
            isBusy = isBusy,
            message = msg?.first,
            isError = msg?.second == true
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaylistsUiState())

    fun addFromUrl(name: String, url: String, autoUpdate: Boolean) {
        viewModelScope.launch {
            busy.value = true
            val outcome = repository.addFromUrl(name, url, autoUpdate)
            message.value = buildMessage(outcome.message, outcome) to !outcome.success
            busy.value = false
        }
    }

    fun addFromUri(name: String, uri: Uri) {
        viewModelScope.launch {
            busy.value = true
            val outcome = repository.addFromUri(name, uri)
            message.value = buildMessage(outcome.message, outcome) to !outcome.success
            busy.value = false
        }
    }

    fun addFromText(name: String, text: String) {
        viewModelScope.launch {
            busy.value = true
            val outcome = repository.addFromText(name, text)
            message.value = buildMessage(outcome.message, outcome) to !outcome.success
            busy.value = false
        }
    }

    fun refresh(id: Long) {
        viewModelScope.launch {
            busy.value = true
            val outcome = repository.refresh(id)
            message.value = buildMessage(outcome.message, outcome) to !outcome.success
            busy.value = false
        }
    }

    fun setEnabled(id: Long, enabled: Boolean) {
        viewModelScope.launch { repository.setEnabled(id, enabled) }
    }

    fun setAutoUpdate(id: Long, auto: Boolean) {
        viewModelScope.launch { repository.setAutoUpdate(id, auto) }
    }

    fun rename(id: Long, name: String) {
        viewModelScope.launch { repository.rename(id, name) }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            repository.delete(id)
            message.value = "Liste silindi." to false
        }
    }

    fun clearMessage() { message.value = null }

    private fun buildMessage(base: String, outcome: com.sabriusta.tv.data.repo.ImportOutcome): String {
        if (!outcome.success) return base
        return buildString {
            append(base)
            append(" TV: ${outcome.tvCount}, Radyo: ${outcome.radioCount}, Film: ${outcome.movieCount}.")
            if (outcome.duplicates > 0) append(" ${outcome.duplicates} tekrar eden yayin ayiklandi.")
            if (outcome.skipped > 0) append(" ${outcome.skipped} hatali satir atlandi.")
        }
    }
}
