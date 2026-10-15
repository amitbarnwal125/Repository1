package com.swara.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.swara.music.playback.PlayerState
import com.swara.music.ui.components.MessageState
import com.swara.music.ui.components.TrackArt
import com.swara.music.util.Format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    state: PlayerState,
    onBack: () -> Unit,
    onSkipTo: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onClear: () -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) { if (state.currentIndex > 0) listState.scrollToItem(state.currentIndex) }
    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
        TopAppBar(
            title = { Text("Up next") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
            actions = { if (state.queue.isNotEmpty()) TextButton(onClick = onClear) { Text("Clear") } },
        )
        if (state.queue.isEmpty()) {
            MessageState(Icons.AutoMirrored.Rounded.QueueMusic, "Your queue is empty", "Use ⋮ → Add to queue on any song.")
            return@Column
        }
        LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 24.dp)) {
            itemsIndexed(state.queue, key = { i, t -> "$i-${t.id}" }) { i, t ->
                val current = i == state.currentIndex
                Row(
                    Modifier.fillMaxWidth()
                        .background(if (current) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
                        .clickable { onSkipTo(i) }
                        .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TrackArt(t, 44.dp, corner = 8.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t.title, fontWeight = if (current) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${t.artist} · ${Format.duration(t.durationMs)}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { onMove(i, i - 1) }, enabled = i > 0) { Icon(Icons.Rounded.KeyboardArrowUp, "Move up") }
                    IconButton(onClick = { onMove(i, i + 1) }, enabled = i < state.queue.lastIndex) { Icon(Icons.Rounded.KeyboardArrowDown, "Move down") }
                    IconButton(onClick = { onRemove(i) }, enabled = !current) { Icon(Icons.Rounded.Close, "Remove from queue") }
                }
            }
        }
    }
}
