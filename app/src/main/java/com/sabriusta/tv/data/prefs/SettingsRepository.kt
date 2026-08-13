package com.sabriusta.tv.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sabri_usta_tv_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class OrientationMode { AUTO, PORTRAIT, LANDSCAPE }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val orientationMode: OrientationMode = OrientationMode.AUTO,
    val autoFullscreen: Boolean = true,
    val wifiOnly: Boolean = false,
    val mobileDataWarning: Boolean = true,
    val bufferSeconds: Int = 30,
    val allowHttp: Boolean = false,
    val legalAccepted: Boolean = false,
    val deduplicate: Boolean = true,
    val catalogSeeded: Boolean = false
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val ORIENTATION = stringPreferencesKey("orientation_mode")
        val AUTO_FULLSCREEN = booleanPreferencesKey("auto_fullscreen")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val MOBILE_WARNING = booleanPreferencesKey("mobile_data_warning")
        val BUFFER = intPreferencesKey("buffer_seconds")
        val ALLOW_HTTP = booleanPreferencesKey("allow_http")
        val LEGAL = booleanPreferencesKey("legal_accepted")
        val DEDUPLICATE = booleanPreferencesKey("deduplicate")
        val CATALOG_SEEDED = booleanPreferencesKey("catalog_seeded")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.DARK,
            orientationMode = prefs[Keys.ORIENTATION]?.let { runCatching { OrientationMode.valueOf(it) }.getOrNull() } ?: OrientationMode.AUTO,
            autoFullscreen = prefs[Keys.AUTO_FULLSCREEN] ?: true,
            wifiOnly = prefs[Keys.WIFI_ONLY] ?: false,
            mobileDataWarning = prefs[Keys.MOBILE_WARNING] ?: true,
            bufferSeconds = prefs[Keys.BUFFER] ?: 30,
            allowHttp = prefs[Keys.ALLOW_HTTP] ?: false,
            legalAccepted = prefs[Keys.LEGAL] ?: false,
            deduplicate = prefs[Keys.DEDUPLICATE] ?: true,
            catalogSeeded = prefs[Keys.CATALOG_SEEDED] ?: false
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.THEME] = mode.name }
    suspend fun setOrientationMode(mode: OrientationMode) = edit { it[Keys.ORIENTATION] = mode.name }
    suspend fun setAutoFullscreen(value: Boolean) = edit { it[Keys.AUTO_FULLSCREEN] = value }
    suspend fun setWifiOnly(value: Boolean) = edit { it[Keys.WIFI_ONLY] = value }
    suspend fun setMobileDataWarning(value: Boolean) = edit { it[Keys.MOBILE_WARNING] = value }
    suspend fun setBufferSeconds(value: Int) = edit { it[Keys.BUFFER] = value.coerceIn(10, 120) }
    suspend fun setAllowHttp(value: Boolean) = edit { it[Keys.ALLOW_HTTP] = value }
    suspend fun setLegalAccepted(value: Boolean) = edit { it[Keys.LEGAL] = value }
    suspend fun setDeduplicate(value: Boolean) = edit { it[Keys.DEDUPLICATE] = value }
    suspend fun setCatalogSeeded(value: Boolean) = edit { it[Keys.CATALOG_SEEDED] = value }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}
