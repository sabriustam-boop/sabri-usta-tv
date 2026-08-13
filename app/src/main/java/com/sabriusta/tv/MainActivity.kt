package com.sabriusta.tv

import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.sabriusta.tv.data.prefs.AppSettings
import com.sabriusta.tv.data.prefs.OrientationMode
import com.sabriusta.tv.data.prefs.SettingsRepository
import com.sabriusta.tv.ui.SabriUstaTvRoot
import com.sabriusta.tv.ui.theme.SabriUstaTvTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Oynatici ekranlarinin resim-icinde-resim moduna gecebilmesi icin. */
val LocalPipController = compositionLocalOf<PipController> { PipController { } }

fun interface PipController {
    fun enterPip()
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository

    private var inPipMode by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        var settings = AppSettings()
        lifecycleScope.launch {
            settingsRepository.settings.collectLatest { newSettings ->
                settings = newSettings
                applyOrientation(newSettings.orientationMode)
            }
        }

        setContent {
            val currentSettings = com.sabriusta.tv.ui.rememberSettingsState(settingsRepository, settings)
            SabriUstaTvTheme(themeMode = currentSettings.themeMode) {
                CompositionLocalProvider(LocalPipController provides PipController { enterPipMode() }) {
                    SabriUstaTvRoot(
                        settings = currentSettings,
                        isInPipMode = inPipMode
                    )
                }
            }
        }
    }

    private fun applyOrientation(mode: OrientationMode) {
        requestedOrientation = when (mode) {
            OrientationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            OrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            OrientationMode.AUTO -> ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        }
    }

    fun enterPipMode() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (!packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE)) return
        runCatching {
            enterPictureInPictureMode(PictureInPictureParams.Builder().build())
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        inPipMode = isInPictureInPictureMode
    }
}
