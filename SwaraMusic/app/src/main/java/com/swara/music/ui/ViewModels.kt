package com.swara.music.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.swara.music.AppContainer
import com.swara.music.container
import com.swara.music.data.model.Collection
import com.swara.music.data.model.Loadable
import com.swara.music.data.model.Track
import com.swara.music.data.repo.DownloadRepository
import com.swara.music.data.repo.MusicRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

@Composable
inline fun <reified VM : ViewModel> appViewModel(key: String? = null, crossinline create: (AppContainer) -> VM): VM {
    val container = LocalContext.current.container
    return viewModel(key = key, factory = viewModelFactory { initializer { create(container) } })
}

fun friendlyError(e: Throwable, online: Boolean): String = when {
    !online -> "You're offline."
    e is IOException && e.message?.contains("Server returned") == true -> "The music service is having trouble (${e.message}). Please try again."
    e is IOException -> "Network problem: couldn't reach the music service."
    else -> e.message ?: "Something went wrong."
}

// ---------------------------------------------------------------------------------------------

class HomeViewModel(private val c: AppContainer) : ViewModel() {
    data class HomeData(
        val featured: List<Track>,
        val jamendoPopular: List<Track>,
        val playlists: List<Collection>,
        val categories: List<Collection>,
        val albums: List<Collection>,
        val artists: List<Collection>,
    )

    private val _state = MutableStateFlow<Loadable<HomeData>>(Loadable.Loading)
    val state: StateFlow<Loadable<HomeData>> = _state.asStateFlow()
    val online = c.network.online
    val downloaded = c.downloads.completedTracks.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        load()
        viewModelScope.launch {
            c.network.online.drop(1).filter { it }.collect { if (_state.value is Loadable.Failed) load() }
        }
        viewModelScope.launch {
            c.settings.jamendoClientId.drop(1).collect { c.music.clearCache(); load() }
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.value = Loadable.Loading
            try {
                val playlists = c.music.curatedPlaylists()
                val curated = playlists.map { p -> async { runCatching { c.music.collectionTracks(p.id) }.getOrDefault(emptyList()) } }
                val jamendo = async { runCatching { c.music.popularOnJamendo() }.getOrDefault(emptyList()) }
                val featured = c.music.featured()
                val all = featured + curated.awaitAll().flatten() + jamendo.await()
                _state.value = Loadable.Ready(
                    HomeData(
                        featured = featured,
                        jamendoPopular = jamendo.await(),
                        playlists = playlists,
                        categories = c.music.commonsCollections(),
                        albums = c.music.jamendoAlbums(jamendo.await()),
                        artists = c.music.artistsFrom(all.distinctBy { it.id }),
                    )
                )
            } catch (e: Exception) {
                _state.value = Loadable.Failed(friendlyError(e, c.network.isOnline), offline = !c.network.isOnline)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(private val c: AppContainer) : ViewModel() {
    val query = MutableStateFlow("")
    val filter = MutableStateFlow(MusicRepository.SourceFilter.ALL)
    val jamendoEnabled get() = c.music.jamendoEnabled
    val online = c.network.online

    val results: StateFlow<Loadable<List<Track>>?> =
        combine(query.debounce(350), filter, c.network.online) { q, f, on -> Triple(q, f, on) }
            .flatMapLatest { (q, f, on) ->
                if (q.isBlank()) flowOf<Loadable<List<Track>>?>(null) else flow<Loadable<List<Track>>?> {
                    emit(Loadable.Loading)
                    if (!on) {
                        val local = c.downloads.completedTracks.first() + c.library.importedTracks.first()
                        emit(Loadable.Ready(c.music.searchOffline(q, local)))
                    } else {
                        try {
                            emit(Loadable.Ready(c.music.search(q, f)))
                        } catch (e: Exception) {
                            emit(Loadable.Failed(friendlyError(e, c.network.isOnline), !c.network.isOnline))
                        }
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun retry() { val q = query.value; query.value = ""; query.value = q }
}

// ---------------------------------------------------------------------------------------------

class CollectionViewModel(private val c: AppContainer, private val id: String) : ViewModel() {
    private val _tracks = MutableStateFlow<Loadable<List<Track>>>(Loadable.Loading)
    val tracks: StateFlow<Loadable<List<Track>>> = _tracks.asStateFlow()
    private val _title = MutableStateFlow(c.music.collection(id)?.title ?: "")
    val title: StateFlow<String> = _title.asStateFlow()
    val subtitle: String = c.music.collection(id)?.subtitle ?: ""
    val isUserPlaylist = id.startsWith("upl:")
    val collection: Collection? = c.music.collection(id)

    init { load() }

    fun load() {
        viewModelScope.launch {
            if (isUserPlaylist) {
                val pid = id.removePrefix("upl:").toLong()
                _title.value = c.library.playlistName(pid) ?: "Playlist"
                c.library.playlistTracks(pid).collect { _tracks.value = Loadable.Ready(it) }
            } else {
                _tracks.value = Loadable.Loading
                _tracks.value = try {
                    Loadable.Ready(c.music.collectionTracks(id))
                } catch (e: Exception) {
                    Loadable.Failed(friendlyError(e, c.network.isOnline), !c.network.isOnline)
                }
            }
        }
    }

    fun removeFromPlaylist(track: Track) {
        if (!isUserPlaylist) return
        viewModelScope.launch { c.library.removeFromPlaylist(id.removePrefix("upl:").toLong(), track.id) }
    }

    fun deletePlaylist(onDone: () -> Unit) {
        if (!isUserPlaylist) return
        viewModelScope.launch { c.library.deletePlaylist(id.removePrefix("upl:").toLong()); onDone() }
    }
}

// ---------------------------------------------------------------------------------------------

class LibraryViewModel(private val c: AppContainer) : ViewModel() {
    val downloads = c.downloads.downloads
    val playlists = c.library.userPlaylists.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val imported = c.library.importedTracks.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _storage = MutableStateFlow(DownloadRepository.StorageInfo(0, 0, 0))
    val storage: StateFlow<DownloadRepository.StorageInfo> = _storage.asStateFlow()

    init {
        viewModelScope.launch {
            downloads.map { list -> list.count { it.status == com.swara.music.data.local.DownloadStatus.DONE } to list.size }
                .collect { _storage.value = c.downloads.storageInfo() }
        }
    }

    fun deleteAll() = viewModelScope.launch { c.downloads.deleteAll() }
    fun createPlaylist(name: String) = viewModelScope.launch { c.library.createPlaylist(name) }
    fun import(uris: List<android.net.Uri>) = viewModelScope.launch {
        val n = c.library.import(uris)
        c.player.message(if (n == 0) "Couldn't import those files" else "Imported $n file${if (n == 1) "" else "s"}")
    }
    fun removeImported(t: Track) = viewModelScope.launch { c.library.removeImported(t) }
}
