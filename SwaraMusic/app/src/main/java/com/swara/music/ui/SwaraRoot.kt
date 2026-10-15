package com.swara.music.ui

import android.net.Uri
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.swara.music.container
import com.swara.music.data.model.Track
import com.swara.music.data.repo.DownloadRepository.StartResult
import com.swara.music.ui.components.AddToPlaylistDialog
import com.swara.music.ui.components.LocalDownloads
import com.swara.music.ui.components.LocalNowPlayingId
import com.swara.music.ui.components.LocalTrackActions
import com.swara.music.ui.components.MiniPlayer
import com.swara.music.ui.components.TrackActions
import com.swara.music.ui.components.TrackDetailsSheet
import com.swara.music.ui.screens.CollectionScreen
import com.swara.music.ui.screens.HomeScreen
import com.swara.music.ui.screens.LibraryScreen
import com.swara.music.ui.screens.LicensesScreen
import com.swara.music.ui.screens.PlayerScreen
import com.swara.music.ui.screens.PrivacyScreen
import com.swara.music.ui.screens.QueueScreen
import com.swara.music.ui.screens.SearchScreen
import com.swara.music.ui.screens.SettingsScreen
import com.swara.music.ui.screens.SourcesScreen
import kotlinx.coroutines.launch

private data class Tab(val route: String, val label: String, val icon: ImageVector)
private val tabs = listOf(
    Tab("home", "Home", Icons.Rounded.Home),
    Tab("search", "Search", Icons.Rounded.Search),
    Tab("library", "Library", Icons.Rounded.LibraryMusic),
)

@Composable
fun SwaraRoot() {
    val c = LocalContext.current.container
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val player by c.player.state.collectAsStateWithLifecycle()
    val downloads by c.downloads.byTrackId.collectAsStateWithLifecycle()
    val playlists by c.library.userPlaylists.collectAsStateWithLifecycle(initialValue = emptyList())
    var detailsFor by remember { mutableStateOf<Track?>(null) }
    var playlistFor by remember { mutableStateOf<Track?>(null) }

    LaunchedEffect(Unit) { c.player.messages.collect { snackbar.showSnackbar(it) } }

    val actions = remember {
        TrackActions(
            play = { list, i -> c.player.play(list, i) },
            playNext = { c.player.playNext(it) },
            addToQueue = { c.player.addToQueue(it) },
            download = { t ->
                scope.launch {
                    when (val r = c.downloads.start(t)) {
                        StartResult.Started -> c.player.message(
                            if (c.settings.wifiOnly.value) "Downloading “${t.title}” (Wi-Fi only)" else "Downloading “${t.title}”"
                        )
                        StartResult.AlreadyDownloaded -> c.player.message("Already downloaded")
                        is StartResult.NotAllowed -> c.player.message(r.reason)
                        is StartResult.Error -> c.player.message(r.message)
                    }
                }
            },
            cancelDownload = { t -> scope.launch { c.downloads.cancel(t.id); c.player.message("Download cancelled") } },
            removeDownload = { t -> scope.launch { c.downloads.delete(t.id); c.player.message("Removed from downloads") } },
            showDetails = { detailsFor = it },
            addToPlaylist = { playlistFor = it },
        )
    }

    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val fullScreen = route == "player" || route == "queue"

    CompositionLocalProvider(
        LocalTrackActions provides actions,
        LocalDownloads provides downloads,
        LocalNowPlayingId provides player.current?.id,
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                if (!fullScreen) {
                    Column {
                        MiniPlayer(player, onOpen = { nav.navigate("player") }, onToggle = c.player::togglePlay, onNext = c.player::next)
                        NavigationBar {
                            tabs.forEach { tab ->
                                val selected = backStack?.destination?.route == tab.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { nav.goTab(tab.route) },
                                    icon = { Icon(tab.icon, null) },
                                    label = { Text(tab.label) },
                                )
                            }
                        }
                    }
                }
            },
        ) { padding ->
            val content = PaddingValues(bottom = padding.calculateBottomPadding())
            val openCollection: (String) -> Unit = { id -> nav.navigate("collection/" + Uri.encode(id)) }
            NavHost(nav, startDestination = "home", modifier = Modifier.fillMaxSize()) {
                composable("home") {
                    HomeScreen(
                        content, openCollection,
                        openSettings = { nav.navigate("settings") },
                        openSources = { nav.navigate("sources") },
                        openLibrary = { nav.goTab("library") },
                    )
                }
                composable("search") { SearchScreen(content, openCollection, openLibrary = { nav.goTab("library") }) }
                composable("library") { LibraryScreen(content, openCollection, openHome = { nav.goTab("home") }) }
                composable("collection/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    CollectionScreen(id, content, onBack = { nav.popBackStack() }, openLibrary = { nav.goTab("library") })
                }
                composable(
                    "player",
                    enterTransition = { slideInVertically { it } },
                    exitTransition = { slideOutVertically { it } },
                ) {
                    PlayerScreen(
                        state = player,
                        onClose = { nav.popBackStack() },
                        onToggle = c.player::togglePlay,
                        onNext = c.player::next,
                        onPrevious = c.player::previous,
                        onSeek = c.player::seekTo,
                        onShuffle = c.player::toggleShuffle,
                        onRepeat = c.player::cycleRepeat,
                        openQueue = { nav.navigate("queue") },
                    )
                }
                composable("queue") {
                    QueueScreen(
                        state = player,
                        onBack = { nav.popBackStack() },
                        onSkipTo = c.player::skipToIndex,
                        onRemove = c.player::removeAt,
                        onMove = c.player::move,
                        onClear = { c.player.clearQueue(); nav.popBackStack("home", inclusive = false) },
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        onBack = { nav.popBackStack() },
                        openSources = { nav.navigate("sources") },
                        openPrivacy = { nav.navigate("privacy") },
                        openLicenses = { nav.navigate("licenses") },
                    )
                }
                composable("sources") { SourcesScreen(onBack = { nav.popBackStack() }) }
                composable("privacy") { PrivacyScreen(onBack = { nav.popBackStack() }) }
                composable("licenses") { LicensesScreen(onBack = { nav.popBackStack() }) }
            }
        }

        detailsFor?.let { t -> TrackDetailsSheet(t, onDismiss = { detailsFor = null }) }
        playlistFor?.let { t ->
            AddToPlaylistDialog(
                track = t,
                playlists = playlists,
                onPick = { id ->
                    playlistFor = null
                    scope.launch {
                        val added = c.library.addToPlaylist(id, t)
                        c.player.message(if (added) "Added to playlist" else "Already in that playlist")
                    }
                },
                onCreate = { name ->
                    playlistFor = null
                    scope.launch {
                        val id = c.library.createPlaylist(name)
                        c.library.addToPlaylist(id, t)
                        c.player.message("Created “$name” and added the song")
                    }
                },
                onDismiss = { playlistFor = null },
            )
        }
    }
}

private fun NavHostController.goTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Suppress("unused") private val unusedDp = 0.dp
