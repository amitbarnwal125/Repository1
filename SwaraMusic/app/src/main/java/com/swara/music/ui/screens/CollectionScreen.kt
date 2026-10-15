package com.swara.music.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.swara.music.data.model.Collection
import com.swara.music.data.model.Loadable
import com.swara.music.ui.CollectionViewModel
import com.swara.music.ui.appViewModel
import com.swara.music.ui.components.CollectionArt
import com.swara.music.ui.components.ErrorState
import com.swara.music.ui.components.LoadingState
import com.swara.music.ui.components.LocalDownloads
import com.swara.music.ui.components.LocalTrackActions
import com.swara.music.ui.components.MessageState
import com.swara.music.ui.components.TrackRow
import com.swara.music.util.Format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(id: String, contentPadding: PaddingValues, onBack: () -> Unit, openLibrary: () -> Unit) {
    val vm = appViewModel(key = "collection-$id") { CollectionViewModel(it, id) }
    val tracks by vm.tracks.collectAsStateWithLifecycle()
    val title by vm.title.collectAsStateWithLifecycle()
    val actions = LocalTrackActions.current
    val downloads = LocalDownloads.current
    var confirmDelete by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
            actions = {
                if (vm.isUserPlaylist) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, "Delete playlist") }
            },
        )
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 16.dp)) {
            item {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    CollectionArt(vm.collection ?: Collection(id, title, "", Collection.Kind.USER_PLAYLIST), 112.dp)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (vm.subtitle.isNotBlank()) {
                            Text(vm.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        (tracks as? Loadable.Ready)?.data?.let { list ->
                            val total = list.sumOf { it.durationMs }
                            Text(
                                "${list.size} songs" + (if (total > 0) " · ${Format.duration(total)}" else ""),
                                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
            when (val s = tracks) {
                is Loadable.Loading -> item { LoadingState() }
                is Loadable.Failed -> item { ErrorState(s.message, s.offline, onRetry = vm::load, onOpenDownloads = openLibrary) }
                is Loadable.Ready -> {
                    val list = s.data
                    if (list.isEmpty()) {
                        item {
                            MessageState(
                                Icons.Rounded.LibraryMusic,
                                if (vm.isUserPlaylist) "This playlist is empty" else "Nothing here yet",
                                if (vm.isUserPlaylist) "Use ⋮ → Add to playlist on any song to fill it."
                                else "No playable, freely licensed audio was found in this collection.",
                            )
                        }
                    } else {
                        item {
                            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { actions.play(list, 0) }) {
                                    Icon(Icons.Rounded.PlayArrow, null); Spacer(Modifier.width(4.dp)); Text("Play")
                                }
                                FilledTonalButton(onClick = { actions.play(list.shuffled(), 0) }) {
                                    Icon(Icons.Rounded.Shuffle, null); Spacer(Modifier.width(4.dp)); Text("Shuffle")
                                }
                                val downloadable = list.filter { it.canDownload && downloads[it.id] == null }
                                if (downloadable.isNotEmpty()) {
                                    OutlinedButton(onClick = { downloadable.forEach(actions.download) }) {
                                        Icon(Icons.Rounded.Download, null); Spacer(Modifier.width(4.dp)); Text("All")
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        itemsIndexed(list, key = { _, t -> t.id }) { i, t ->
                            TrackRow(
                                t, onClick = { actions.play(list, i) },
                                extraMenu = if (vm.isUserPlaylist) listOf("Remove from this playlist" to { vm.removeFromPlaylist(t) }) else emptyList(),
                            )
                        }
                    }
                }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete “$title”?") },
            text = { Text("The playlist is removed. Downloaded songs stay in your library.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; vm.deletePlaylist(onBack) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
