package com.swara.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.swara.music.data.local.DownloadEntity
import com.swara.music.data.local.DownloadStatus
import com.swara.music.data.model.SourceType
import com.swara.music.data.model.Track
import com.swara.music.util.Format

@Composable
fun TrackRow(
    track: Track,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    extraMenu: List<Pair<String, () -> Unit>> = emptyList(),
) {
    val actions = LocalTrackActions.current
    val download = LocalDownloads.current[track.id]
    val isCurrent = LocalNowPlayingId.current == track.id
    var menu by remember { mutableStateOf(false) }

    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            TrackArt(track, 52.dp)
            if (isCurrent) {
                Box(
                    Modifier.size(52.dp).clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.GraphicEq, "Now playing", tint = MaterialTheme.colorScheme.primary) }
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                buildString {
                    append(track.artist)
                    if (track.durationMs > 0) append(" · ").append(Format.duration(track.durationMs))
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            LicenseChipRow(track, download)
        }
        DownloadButton(track, download)
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "More options for ${track.title}")
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Play next") }, leadingIcon = { Icon(Icons.Rounded.PlaylistPlay, null) },
                    onClick = { menu = false; actions.playNext(track) })
                DropdownMenuItem(text = { Text("Add to queue") }, leadingIcon = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, null) },
                    onClick = { menu = false; actions.addToQueue(track) })
                DropdownMenuItem(text = { Text("Add to playlist") }, leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null) },
                    onClick = { menu = false; actions.addToPlaylist(track) })
                if (download?.status == DownloadStatus.DONE) {
                    DropdownMenuItem(text = { Text("Remove download") }, leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                        onClick = { menu = false; actions.removeDownload(track) })
                }
                DropdownMenuItem(text = { Text("Details & licence") }, leadingIcon = { Icon(Icons.Rounded.Info, null) },
                    onClick = { menu = false; actions.showDetails(track) })
                if (extraMenu.isNotEmpty()) HorizontalDivider()
                extraMenu.forEach { (label, action) ->
                    DropdownMenuItem(text = { Text(label) }, onClick = { menu = false; action() })
                }
            }
        }
    }
}

@Composable
private fun LicenseChipRow(track: Track, download: DownloadEntity?) {
    val text = when {
        download?.status == DownloadStatus.FAILED -> download.error ?: "Download failed"
        download?.status == DownloadStatus.PAUSED -> download.error ?: "Paused"
        download?.isActive == true -> "Downloading " + (if (download.totalBytes > 0) "${(download.progress * 100).toInt()}%" else "…")
        download?.status == DownloadStatus.DONE -> "Saved offline · ${track.license.name}"
        track.sourceType == SourceType.LOCAL -> "On this device"
        track.canDownload -> "${track.license.name} · offline OK"
        else -> "${track.license.name} · stream only"
    }
    val color = when {
        download?.status == DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
        download?.status == DownloadStatus.DONE -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.outline
    }
    Text(text, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
fun DownloadButton(track: Track, download: DownloadEntity?) {
    val actions = LocalTrackActions.current
    when {
        track.sourceType == SourceType.LOCAL -> Unit
        download?.status == DownloadStatus.DONE ->
            IconButton(onClick = { actions.showDetails(track) }, modifier = Modifier.semantics { contentDescription = "Downloaded" }) {
                Icon(Icons.Rounded.DownloadDone, null, tint = MaterialTheme.colorScheme.tertiary)
            }
        download?.isActive == true ->
            IconButton(onClick = { actions.cancelDownload(track) }, modifier = Modifier.semantics { contentDescription = "Cancel download" }) {
                Box(contentAlignment = Alignment.Center) {
                    if (download.totalBytes > 0 && download.bytesDownloaded > 0) {
                        CircularProgressIndicator(progress = { download.progress }, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                    } else {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                    }
                    Icon(Icons.Rounded.Close, null, modifier = Modifier.size(14.dp))
                }
            }
        download?.status == DownloadStatus.FAILED ->
            IconButton(onClick = { actions.download(track) }, modifier = Modifier.semantics { contentDescription = "Retry download" }) {
                Icon(Icons.Rounded.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
            }
        track.canDownload ->
            IconButton(onClick = { actions.download(track) }, modifier = Modifier.semantics { contentDescription = "Download ${track.title}" }) {
                Icon(Icons.Rounded.Download, null)
            }
        else -> Unit
    }
}
