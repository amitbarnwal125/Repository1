package com.swara.music

import android.app.Application
import android.content.Context
import com.swara.music.data.NetworkMonitor
import com.swara.music.data.Settings
import com.swara.music.data.local.AppDatabase
import com.swara.music.data.remote.CommonsProvider
import com.swara.music.data.remote.Http
import com.swara.music.data.remote.JamendoProvider
import com.swara.music.data.repo.DownloadRepository
import com.swara.music.data.repo.LibraryRepository
import com.swara.music.data.repo.MusicRepository
import com.swara.music.playback.PlayerConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class SwaraApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Manual dependency injection: one place that wires the app together. */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val settings = Settings(context)
    val network = NetworkMonitor(context)
    val database = AppDatabase.create(context)
    private val http = Http.client(context.cacheDir)

    val commons = CommonsProvider(http)
    val jamendo = JamendoProvider(http) { settings.jamendoClientId.value }
    val music = MusicRepository(commons, jamendo)
    val downloads = DownloadRepository(context, database, settings, appScope)
    val library = LibraryRepository(context, database)
    val player = PlayerConnection(context, appScope, downloads, network)
}

val Context.container: AppContainer get() = (applicationContext as SwaraApp).container
