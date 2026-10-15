package com.swara.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.swara.music.data.local.DownloadStatus
import com.swara.music.data.local.PlaylistWithCount
import com.swara.music.data.model.SourceType
import com.swara.music.data.model.Track
import com.swara.music.util.Format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackDetailsSheet(track: Track, onDismiss: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val actions = LocalTrackActions.current
    val download = LocalDownloads.current[track.id]
    val uri = LocalUriHandler.current

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TrackArt(track, 88.dp, corner = 16.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(track.title, style = MaterialTheme.typography.titleLarge, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    track.album?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            DetailRow("Source", track.sourceType.label)
            if (track.durationMs > 0) DetailRow("Length", Format.duration(track.durationMs))
            if (track.sizeBytes > 0) DetailRow("File size", Format.bytes(track.sizeBytes))
            track.mimeType?.let { DetailRow("Format", it) }
            DetailRow("Licence", track.license.name)

            Spacer(Modifier.height(12.dp))
            val offline = track.canDownload
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    if (offline) Icons.Rounded.CheckCircle else Icons.Rounded.Block, null,
                    tint = if (offline) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        when {
                            track.sourceType == SourceType.LOCAL -> "Stored on your phone"
                            offline -> "Offline download permitted"
                            else -> "Streaming only"
                        },
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(track.license.offlineNote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Attribution", style = MaterialTheme.typography.titleSmall)
            Text(track.license.attribution, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                track.sourcePageUrl?.let { url ->
                    OutlinedButton(onClick = { runCatching { uri.openUri(url) } }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Source page")
                    }
                }
                track.license.url?.let { url ->
                    OutlinedButton(onClick = { runCatching { uri.openUri(url) } }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Licence")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            when {
                download?.status == DownloadStatus.DONE -> OutlinedButton(
                    onClick = { actions.removeDownload(track) }, modifier = Modifier.fillMaxWidth()
                ) { Icon(Icons.Rounded.Delete, null); Spacer(Modifier.width(8.dp)); Text("Remove download (${Format.bytes(download.totalBytes)})") }
                download?.isActive == true -> OutlinedButton(
                    onClick = { actions.cancelDownload(track) }, modifier = Modifier.fillMaxWidth()
                ) { Text("Cancel download (${(download.progress * 100).toInt()}%)") }
                track.canDownload -> Button(onClick = { actions.download(track) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Download, null); Spacer(Modifier.width(8.dp)); Text("Download for offline")
                }
                else -> Unit
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(96.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
fun AddToPlaylistDialog(
    track: Track,
    playlists: List<PlaylistWithCount>,
    onPick: (Long) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var creating by remember { mutableStateOf(playlists.isEmpty()) }
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (creating) "New playlist" else "Add to playlist") },
        text = {
            if (creating) {
                Column {
                    Text("“${track.title}” will be added to it.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text("Playlist name") })
                }
            } else {
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    item {
                        Row(
                            Modifier.fillMaxWidth().clickable { creating = true }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text("New playlist", color = MaterialTheme.colorScheme.primary)
                        }
                        HorizontalDivider()
                    }
                    items(playlists, key = { it.id }) { p ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onPick(p.id) }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.LibraryMusic, null)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(p.name)
                                Text("${p.count} songs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (creating) TextButton(onClick = { onCreate(name) }, enabled = name.isNotBlank()) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
