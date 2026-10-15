package com.swara.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.swara.music.data.model.Loadable
import com.swara.music.ui.HomeViewModel
import com.swara.music.ui.appViewModel
import com.swara.music.ui.components.CollectionCard
import com.swara.music.ui.components.ErrorState
import com.swara.music.ui.components.LoadingState
import com.swara.music.ui.components.LocalTrackActions
import com.swara.music.ui.components.OfflineBanner
import com.swara.music.ui.components.SectionHeader
import com.swara.music.ui.components.TrackCard
import com.swara.music.ui.components.TrackRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    openCollection: (String) -> Unit,
    openSettings: () -> Unit,
    openSources: () -> Unit,
    openLibrary: () -> Unit,
) {
    val vm = appViewModel { HomeViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val online by vm.online.collectAsStateWithLifecycle()
    val downloaded by vm.downloaded.collectAsStateWithLifecycle()
    val actions = LocalTrackActions.current

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text("Swara", fontWeight = FontWeight.Bold)
                    Text("Free & legal Indian music", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            actions = {
                IconButton(onClick = openSources) { Icon(Icons.Rounded.Gavel, "Music sources and licences") }
                IconButton(onClick = openSettings) { Icon(Icons.Rounded.Settings, "Settings") }
            },
        )
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 16.dp)) {
            if (!online) item { OfflineBanner() }
            when (val s = state) {
                is Loadable.Loading -> item { LoadingState("Finding freely licensed music…") }
                is Loadable.Failed -> {
                    item { ErrorState(s.message, s.offline, onRetry = vm::load, onOpenDownloads = openLibrary) }
                    if (downloaded.isNotEmpty()) {
                        item { SectionHeader("Your downloads", "Available offline") }
                        itemsIndexed(downloaded, key = { _, t -> "dl-" + t.id }) { i, t -> TrackRow(t, onClick = { actions.play(downloaded, i) }) }
                    }
                }
                is Loadable.Ready -> {
                    val d = s.data
                    item {
                        Hero(
                            onPlay = { if (d.featured.isNotEmpty()) actions.play(d.featured, 0) },
                            onShuffle = { if (d.featured.isNotEmpty()) actions.play(d.featured.shuffled(), 0) },
                            onLearn = openSources,
                        )
                    }
                    if (d.featured.isNotEmpty()) {
                        item { SectionHeader("Featured tracks", "Hand-picked, all free to stream and download") }
                        item {
                            LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                itemsIndexed(d.featured, key = { _, t -> t.id }) { i, t -> TrackCard(t, onClick = { actions.play(d.featured, i) }) }
                            }
                        }
                    }
                    item { SectionHeader("Playlists", "Curated mixes") }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(d.playlists, key = { it.id }) { c -> CollectionCard(c, onClick = { openCollection(c.id) }) }
                        }
                    }
                    if (d.jamendoPopular.isNotEmpty()) {
                        item { SectionHeader("Popular on Jamendo", "Indian & Bollywood-tagged · stream only") }
                        itemsIndexed(d.jamendoPopular.take(8), key = { _, t -> "jp-" + t.id }) { i, t ->
                            TrackRow(t, onClick = { actions.play(d.jamendoPopular, i) })
                        }
                    }
                    if (d.albums.isNotEmpty()) {
                        item { SectionHeader("Albums") }
                        item {
                            LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(d.albums, key = { it.id }) { c -> CollectionCard(c, onClick = { openCollection(c.id) }) }
                            }
                        }
                    }
                    item { SectionHeader("Collections", "Live from Wikimedia Commons — grows as people upload") }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(d.categories, key = { it.id }) { c -> CollectionCard(c, onClick = { openCollection(c.id) }) }
                        }
                    }
                    if (d.artists.isNotEmpty()) {
                        item { SectionHeader("Artists") }
                        item {
                            LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(d.artists, key = { it.id }) { c -> CollectionCard(c, onClick = { openCollection(c.id) }, width = 120.dp) }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun Hero(onPlay: () -> Unit, onShuffle: () -> Unit, onLearn: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFE8711A), Color(0xFFB8336A), Color(0xFF4A1D63))))
            .padding(20.dp)
    ) {
        Column {
            Text("Bollywood-style sounds,\nzero piracy.", style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Spacer(Modifier.height(6.dp))
            Text(
                "Public-domain film songs, ragas and modern Indian instrumentals — free to stream and keep offline.",
                style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f),
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPlay) { Icon(Icons.Rounded.PlayArrow, null); Spacer(Modifier.width(4.dp)); Text("Play") }
                FilledTonalButton(onClick = onShuffle) { Icon(Icons.Rounded.Shuffle, null); Spacer(Modifier.width(4.dp)); Text("Shuffle") }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Why no new chart hits? Tap ⚖ to see the licences.",
                style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
