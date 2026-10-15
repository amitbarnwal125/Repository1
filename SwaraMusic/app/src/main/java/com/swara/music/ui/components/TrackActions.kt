package com.swara.music.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.compositionLocalOf
import com.swara.music.data.local.DownloadEntity
import com.swara.music.data.model.Track

/** Everything a track row can do; provided once at the root so every screen behaves the same. */
class TrackActions(
    val play: (tracks: List<Track>, index: Int) -> Unit,
    val playNext: (Track) -> Unit,
    val addToQueue: (Track) -> Unit,
    val download: (Track) -> Unit,
    val cancelDownload: (Track) -> Unit,
    val removeDownload: (Track) -> Unit,
    val showDetails: (Track) -> Unit,
    val addToPlaylist: (Track) -> Unit,
)

val LocalTrackActions = staticCompositionLocalOf<TrackActions> { error("TrackActions not provided") }
val LocalDownloads = compositionLocalOf<Map<String, DownloadEntity>> { emptyMap() }
val LocalNowPlayingId = compositionLocalOf<String?> { null }
