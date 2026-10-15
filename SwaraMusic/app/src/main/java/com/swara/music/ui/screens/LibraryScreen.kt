package com.swara.music.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.swara.music.data.local.DownloadStatus
import com.swara.music.data.model.Collection
import com.swara.music.data.model.Track
import com.swara.music.ui.LibraryViewModel
import com.swara.music.ui.appViewModel
import com.swara.music.ui.components.CollectionArt
import com.swara.music.ui.components.LocalTrackActions
import com.swara.music.ui.components.MessageState
import com.swara.music.ui.components.TrackRow
import com.swara.music.util.Format

@Composable
fun LibraryScreen(contentPadding: PaddingValues, openCollection: (String) -> Unit, openHome: () -> Unit) {
    val vm = appViewModel { LibraryViewModel(it) }
    val downloads by vm.downloads.collectAsStateWithLifecycle()
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val imported by vm.imported.collectAsStateWithLifecycle()
    val storage by vm.storage.collectAsStateWithLifecycle()
    val actions = LocalTrackActions.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    var newPlaylist by rememberSaveable { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) vm.import(uris)
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text("Your library", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp))
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Downloads") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Playlists") })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("On device") })
        }
        val bottom = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 16.dp)
        when (tab) {
            0 -> {
                val tracks = downloads.mapNotNull { e -> Track.fromJson(e.trackJson)?.let { e to it } }
                val done = tracks.filter { it.first.status == DownloadStatus.DONE }.map { it.second }
                LazyColumn(Modifier.fillMaxSize(), contentPadding = bottom) {
                    item { StorageCard(storage.usedBytes, storage.freeBytes, done.size, onClear = { confirmClear = true }) }
                    if (tracks.isEmpty()) {
                        item {
                            MessageState(
                                Icons.Rounded.Download, "No downloads yet",
                                "Tap the download arrow on any song marked “offline OK”. Songs are saved inside the app and play without internet.",
                                primaryAction = "Discover music" to openHome,
                            )
                        }
                    } else {
                        if (done.isNotEmpty()) {
                            item {
                                Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { actions.play(done, 0) }) { Icon(Icons.Rounded.PlayArrow, null); Spacer(Modifier.width(4.dp)); Text("Play all") }
                                    FilledTonalButton(onClick = { actions.play(done.shuffled(), 0) }) { Text("Shuffle") }
                                }
                            }
                        }
                        items(tracks, key = { it.second.id }) { (entity, track) ->
                            TrackRow(track, onClick = {
                                if (entity.status == DownloadStatus.DONE) actions.play(done, done.indexOf(track).coerceAtLeast(0))
                                else actions.play(listOf(track), 0)
                            })
                        }
                    }
                }
            }
            1 -> LazyColumn(Modifier.fillMaxSize(), contentPadding = bottom) {
                item {
                    ListItem(
                        headlineContent = { Text("New playlist") },
                        leadingContent = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable { newPlaylist = true },
                    )
                }
                if (playlists.isEmpty()) {
                    item {
                        MessageState(Icons.Rounded.LibraryMusic, "No playlists yet", "Create one here, or use ⋮ → Add to playlist on any song.")
                    }
                }
                items(playlists, key = { it.id }) { p ->
                    val c = Collection("upl:${p.id}", p.name, "${p.count} songs", Collection.Kind.USER_PLAYLIST)
                    ListItem(
                        headlineContent = { Text(p.name) },
                        supportingContent = { Text("${p.count} songs") },
                        leadingContent = { CollectionArt(c, 52.dp, corner = 10.dp) },
                        modifier = Modifier.clickable { openCollection(c.id) },
                    )
                }
            }
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = bottom) {
                item {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Play audio files you already have on your phone. Swara only reads the files you pick; " +
                                "you're responsible for having the rights to them.",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { picker.launch(arrayOf("audio/*")) }) {
                            Icon(Icons.Rounded.FolderOpen, null); Spacer(Modifier.width(8.dp)); Text("Import audio files")
                        }
                    }
                }
                if (imported.isEmpty()) {
                    item { MessageState(Icons.Rounded.Add, "No imported files", "Imported songs appear here and work offline.") }
                } else {
                    itemsIndexed(imported, key = { _, t -> t.id }) { i, t ->
                        TrackRow(t, onClick = { actions.play(imported, i) }, extraMenu = listOf("Remove from Swara" to { vm.removeImported(t) }))
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Delete all downloads?") },
            text = { Text("This frees ${Format.bytes(storage.usedBytes)}. You can download the songs again later.") },
            confirmButton = { TextButton(onClick = { confirmClear = false; vm.deleteAll() }) { Text("Delete all") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
    if (newPlaylist) {
        var name by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { newPlaylist = false },
            title = { Text("New playlist") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text("Name") }) },
            confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { vm.createPlaylist(name); newPlaylist = false }) { Text("Create") } },
            dismissButton = { TextButton(onClick = { newPlaylist = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun StorageCard(used: Long, free: Long, count: Int, onClear: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Storage, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Offline storage", style = MaterialTheme.typography.titleMedium)
                Text(
                    "$count songs · ${Format.bytes(used)} used · ${Format.bytes(free)} free on phone",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        val fraction = if (used + free > 0) used.toFloat() / (used + free) else 0f
        LinearProgressIndicator(progress = { fraction.coerceAtLeast(if (used > 0) 0.02f else 0f) }, modifier = Modifier.fillMaxWidth())
        if (used > 0) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onClear) { Icon(Icons.Rounded.DeleteSweep, null); Spacer(Modifier.width(6.dp)); Text("Delete all downloads") }
        }
    }
}
