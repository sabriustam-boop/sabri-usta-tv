package com.sabriusta.tv.ui.legal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sabriusta.tv.data.prefs.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LegalViewModel @Inject constructor(
    private val settings: SettingsRepository
) : ViewModel() {
    fun accept() {
        viewModelScope.launch { settings.setLegalAccepted(true) }
    }
}
