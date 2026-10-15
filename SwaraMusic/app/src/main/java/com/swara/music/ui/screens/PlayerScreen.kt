package com.swara.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.media3.common.Player
import com.swara.music.playback.PlayerState
import com.swara.music.ui.components.DownloadButton
import com.swara.music.ui.components.LocalDownloads
import com.swara.music.ui.components.LocalTrackActions
import com.swara.music.ui.components.MessageState
import com.swara.music.ui.components.TrackArt
import com.swara.music.util.Format

@Composable
fun PlayerScreen(
    state: PlayerState,
    onClose: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    openQueue: () -> Unit,
) {
    val track = state.current
    val actions = LocalTrackActions.current
    val download = track?.let { LocalDownloads.current[it.id] }
    var scrubbing by remember { mutableStateOf<Float?>(null) }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.background))
        )
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Rounded.KeyboardArrowDown, "Close player") }
                Text(
                    "Now playing", style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = openQueue) { Icon(Icons.AutoMirrored.Rounded.QueueMusic, "Queue") }
            }
            if (track == null) {
                MessageState(Icons.Rounded.MusicOff, "Nothing playing", "Pick a song to start listening.", primaryAction = "Back" to onClose)
                return@Column
            }
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                val size = min(maxWidth, maxHeight) - 16.dp
                TrackArt(track, size, corner = 28.dp)
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(track.title, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                DownloadButton(track, download)
                IconButton(onClick = { actions.addToPlaylist(track) }) { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, "Add to playlist") }
            }
            // Source + licence chip: always visible while playing.
            Row(
                Modifier.padding(top = 8.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { actions.showDetails(track) }.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Info, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(6.dp))
                Text(
                    "${track.sourceType.label} · ${track.license.name}",
                    style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(12.dp))
            val duration = state.durationMs.coerceAtLeast(1)
            val position = scrubbing?.let { (it * duration).toLong() } ?: state.positionMs
            Slider(
                value = (position.toFloat() / duration).coerceIn(0f, 1f),
                onValueChange = { scrubbing = it },
                onValueChangeFinished = { scrubbing?.let { onSeek((it * duration).toLong()) }; scrubbing = null },
                enabled = state.durationMs > 0,
            )
            Row(Modifier.fillMaxWidth()) {
                Text(Format.duration(position), style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.weight(1f))
                Text(Format.duration(state.durationMs), style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onShuffle) {
                    Icon(Icons.Rounded.Shuffle, if (state.shuffle) "Shuffle on" else "Shuffle off",
                        tint = if (state.shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) { Icon(Icons.Rounded.SkipPrevious, "Previous", Modifier.size(36.dp)) }
                FilledIconButton(
                    onClick = onToggle, modifier = Modifier.size(76.dp), shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    when {
                        state.isBuffering && !state.isPlaying -> CircularProgressIndicator(Modifier.size(32.dp), color = MaterialTheme.colorScheme.onPrimary)
                        state.isPlaying -> Icon(Icons.Rounded.Pause, "Pause", Modifier.size(40.dp))
                        else -> Icon(Icons.Rounded.PlayArrow, "Play", Modifier.size(40.dp))
                    }
                }
                IconButton(onClick = onNext, enabled = state.hasNext, modifier = Modifier.size(56.dp)) { Icon(Icons.Rounded.SkipNext, "Next", Modifier.size(36.dp)) }
                IconButton(onClick = onRepeat) {
                    val (icon, label) = when (state.repeatMode) {
                        Player.REPEAT_MODE_ONE -> Icons.Rounded.RepeatOne to "Repeat one"
                        Player.REPEAT_MODE_ALL -> Icons.Rounded.Repeat to "Repeat all"
                        else -> Icons.Rounded.Repeat to "Repeat off"
                    }
                    Icon(icon, label, tint = if (state.repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
