package com.sabriusta.tv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.sabriusta.tv.data.prefs.AppSettings
import com.sabriusta.tv.data.prefs.SettingsRepository

@Composable
fun rememberSettingsState(
    repository: SettingsRepository,
    initial: AppSettings
): AppSettings {
    val flow = remember(repository) { repository.settings }
    val state: State<AppSettings> = flow.collectAsState(initial = initial)
    val value by state
    return value
}
