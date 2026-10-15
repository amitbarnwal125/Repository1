package com.swara.music.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.swara.music.data.model.Loadable
import com.swara.music.data.remote.Catalog
import com.swara.music.data.repo.MusicRepository.SourceFilter
import com.swara.music.ui.SearchViewModel
import com.swara.music.ui.appViewModel
import com.swara.music.ui.components.ErrorState
import com.swara.music.ui.components.LoadingState
import com.swara.music.ui.components.LocalTrackActions
import com.swara.music.ui.components.MessageState
import com.swara.music.ui.components.OfflineBanner
import com.swara.music.ui.components.SectionHeader
import com.swara.music.ui.components.TrackRow

private val suggestions = listOf("sitar", "raga", "bhajan", "film song", "Hindi", "tabla", "qawwali", "veena", "dhol", "ghazal", "Rabindra")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(contentPadding: PaddingValues, openCollection: (String) -> Unit, openLibrary: () -> Unit) {
    val vm = appViewModel { SearchViewModel(it) }
    val query by vm.query.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val results by vm.results.collectAsStateWithLifecycle()
    val online by vm.online.collectAsStateWithLifecycle()
    val actions = LocalTrackActions.current
    val focus = LocalFocusManager.current

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text("Search", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { vm.query.value = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text("Songs, artists, ragas, instruments…") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { vm.query.value = "" }) { Icon(Icons.Rounded.Close, "Clear search") }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
        )
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = filter == SourceFilter.ALL, onClick = { vm.filter.value = SourceFilter.ALL }, label = { Text("All sources") })
            FilterChip(selected = filter == SourceFilter.COMMONS, onClick = { vm.filter.value = SourceFilter.COMMONS }, label = { Text("Commons") })
            if (vm.jamendoEnabled) {
                FilterChip(selected = filter == SourceFilter.JAMENDO, onClick = { vm.filter.value = SourceFilter.JAMENDO }, label = { Text("Jamendo") })
            }
        }
        if (!online) OfflineBanner()

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 16.dp)) {
            when (val r = results) {
                null -> {
                    item { SectionHeader("Try searching for") }
                    item {
                        FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            suggestions.forEach { s -> AssistChip(onClick = { vm.query.value = s }, label = { Text(s) }) }
                        }
                    }
                    item { SectionHeader("Browse collections") }
                    Catalog.categories.forEach { cat ->
                        item(key = cat.category) {
                            androidx.compose.material3.ListItem(
                                headlineContent = { Text(cat.title) },
                                supportingContent = { Text(cat.subtitle) },
                                modifier = Modifier.clickable { openCollection("cat:${cat.category}") }.padding(horizontal = 4.dp),
                            )
                        }
                    }
                }
                is Loadable.Loading -> item { LoadingState("Searching…") }
                is Loadable.Failed -> item { ErrorState(r.message, r.offline, onRetry = vm::retry, onOpenDownloads = openLibrary) }
                is Loadable.Ready -> {
                    if (r.data.isEmpty()) {
                        item {
                            MessageState(
                                Icons.Rounded.SearchOff,
                                "No matches for “$query”",
                                if (online) "Only freely licensed music is searchable here, so mainstream film hits won't appear. Try an instrument, raga or style."
                                else "While offline, search only looks through your downloads and imported files.",
                            )
                        }
                    } else {
                        item {
                            Text(
                                "${r.data.size} results", style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                        itemsIndexed(r.data, key = { _, t -> t.id }) { i, t -> TrackRow(t, onClick = { actions.play(r.data, i) }) }
                    }
                }
            }
        }
    }
}
