package com.swara.music.data.repo

import com.swara.music.data.model.Collection
import com.swara.music.data.model.SourceType
import com.swara.music.data.model.Track
import com.swara.music.data.remote.Catalog
import com.swara.music.data.remote.CommonsProvider
import com.swara.music.data.remote.JamendoProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.ConcurrentHashMap

/** Single entry point for browsing and searching every licensed source. */
class MusicRepository(
    private val commons: CommonsProvider,
    private val jamendo: JamendoProvider,
) {
    private val cache = ConcurrentHashMap<String, Pair<Long, List<Track>>>()
    private val collections = ConcurrentHashMap<String, Collection>()
    private val ttlMs = 30 * 60 * 1000L

    val jamendoEnabled: Boolean get() = jamendo.isConfigured

    private suspend fun cached(key: String, load: suspend () -> List<Track>): List<Track> {
        val hit = cache[key]
        if (hit != null && System.currentTimeMillis() - hit.first < ttlMs && hit.second.isNotEmpty()) return hit.second
        val fresh = load()
        cache[key] = System.currentTimeMillis() to fresh
        return fresh
    }

    fun clearCache() = cache.clear()

    suspend fun featured(): List<Track> = cached("featured") { commons.byTitles(Catalog.featured, "Featured") }

    suspend fun popularOnJamendo(): List<Track> =
        if (!jamendo.isConfigured) emptyList() else cached("jamendo-popular") { jamendo.popularIndian() }

    fun curatedPlaylists(): List<Collection> = Catalog.playlists.map {
        Collection("pl:${it.id}", it.title, it.subtitle, Collection.Kind.PLAYLIST).also(::remember)
    }

    fun commonsCollections(): List<Collection> = Catalog.categories.map {
        Collection("cat:${it.category}", it.title, it.subtitle, Collection.Kind.CATEGORY).also(::remember)
    }

    /** Albums from Jamendo popular tracks (if configured). */
    fun jamendoAlbums(tracks: List<Track>): List<Collection> = tracks
        .filter { it.albumId != null && !it.album.isNullOrBlank() }
        .distinctBy { it.albumId }
        .take(12)
        .map {
            Collection("jalbum:${it.albumId}", it.album!!, it.artist, Collection.Kind.ALBUM, it.artworkUrl).also(::remember)
        }

    /** Artists derived from what has been loaded, most tracks first. */
    fun artistsFrom(tracks: List<Track>): List<Collection> = tracks
        .filter { it.artist != "Unknown artist" }
        .groupBy { it.artistId?.let { id -> "jartist:$id" } ?: "artist:${it.artist}" }
        .entries
        .sortedByDescending { it.value.size }
        .take(12)
        .map { (id, list) ->
            val first = list.first()
            Collection(id, first.artist, "${list.size} track${if (list.size == 1) "" else "s"} · ${first.sourceType.label}",
                Collection.Kind.ARTIST, first.artworkUrl).also(::remember)
        }

    fun remember(c: Collection) { collections[c.id] = c }
    fun collection(id: String): Collection? = collections[id]

    suspend fun collectionTracks(id: String): List<Track> = when {
        id.startsWith("pl:") -> {
            val pl = Catalog.playlists.first { it.id == id.removePrefix("pl:") }
            cached(id) { commons.byTitles(pl.files, pl.title) }
        }
        id.startsWith("cat:") -> {
            val cat = id.removePrefix("cat:")
            val title = Catalog.categories.firstOrNull { it.category == cat }?.title ?: cat
            cached(id) { commons.category(cat, title) }
        }
        id.startsWith("jalbum:") -> cached(id) { jamendo.album(id.removePrefix("jalbum:")) }
        id.startsWith("jartist:") -> cached(id) { jamendo.artist(id.removePrefix("jartist:")) }
        id.startsWith("artist:") -> {
            val name = id.removePrefix("artist:")
            val known = cache.values.flatMap { it.second }.filter { it.artist == name }.distinctBy { it.id }
            known.ifEmpty { commons.search("\"$name\"") }
        }
        else -> emptyList()
    }

    enum class SourceFilter { ALL, COMMONS, JAMENDO }

    suspend fun search(query: String, filter: SourceFilter): List<Track> = coroutineScope {
        val q = query.trim()
        if (q.isEmpty()) return@coroutineScope emptyList()
        val c = if (filter != SourceFilter.JAMENDO) async { commons.search(q) } else null
        val j = if (filter != SourceFilter.COMMONS && jamendo.isConfigured) async { jamendo.search(q) } else null
        val commonsResult = c?.let { runCatching { it.await() } }
        val jamendoResult = j?.let { runCatching { it.await() } }
        val results = (commonsResult?.getOrNull().orEmpty() + jamendoResult?.getOrNull().orEmpty())
        // Surface an error only when every source we asked failed.
        val errors = listOfNotNull(commonsResult?.exceptionOrNull(), jamendoResult?.exceptionOrNull())
        if (results.isEmpty() && errors.isNotEmpty()) throw errors.first()
        results.distinctBy { it.id }
    }

    /** Local filter over downloads and imported files — works offline. */
    fun searchOffline(query: String, tracks: List<Track>): List<Track> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return tracks.filter { it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) }
    }

    fun isRemote(track: Track) = track.sourceType != SourceType.LOCAL
}
