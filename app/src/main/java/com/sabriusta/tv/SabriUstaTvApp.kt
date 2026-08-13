package com.sabriusta.tv

import android.app.Application
import com.sabriusta.tv.data.repo.PlaylistRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SabriUstaTvApp : Application() {

    @Inject lateinit var playlistRepository: PlaylistRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            runCatching { playlistRepository.seedStarterCatalogIfNeeded() }
            runCatching { playlistRepository.refreshAutoUpdatable() }
        }
    }
}
