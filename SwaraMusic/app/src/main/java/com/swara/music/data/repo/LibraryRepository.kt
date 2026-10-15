package com.swara.music.data.repo

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.swara.music.data.local.AppDatabase
import com.swara.music.data.local.ImportedEntity
import com.swara.music.data.local.PlaylistEntity
import com.swara.music.data.local.PlaylistItemEntity
import com.swara.music.data.local.PlaylistWithCount
import com.swara.music.data.model.LicenseInfo
import com.swara.music.data.model.SourceType
import com.swara.music.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** User playlists and audio files the user imports from their own device. */
class LibraryRepository(private val context: Context, db: AppDatabase) {
    private val playlists = db.playlists()
    private val imported = db.imported()

    val userPlaylists: Flow<List<PlaylistWithCount>> = playlists.observePlaylists()

    fun playlistTracks(id: Long): Flow<List<Track>> =
        playlists.observeItems(id).map { items -> items.mapNotNull { Track.fromJson(it.trackJson) } }

    suspend fun playlistName(id: Long): String? = playlists.playlist(id)?.name

    suspend fun createPlaylist(name: String): Long =
        playlists.insertPlaylist(PlaylistEntity(name = name.trim().ifBlank { "My playlist" }, createdAt = System.currentTimeMillis()))

    /** @return false if it was already in the playlist. */
    suspend fun addToPlaylist(playlistId: Long, track: Track): Boolean {
        if (playlists.contains(playlistId, track.id) > 0) return false
        playlists.insertItem(PlaylistItemEntity(playlistId = playlistId, trackId = track.id, trackJson = track.toJson(), addedAt = System.currentTimeMillis()))
        return true
    }

    suspend fun removeFromPlaylist(playlistId: Long, trackId: String) = playlists.removeItem(playlistId, trackId)

    suspend fun deletePlaylist(playlistId: Long) {
        playlists.clearItems(playlistId)
        playlists.deletePlaylistRow(playlistId)
    }

    val importedTracks: Flow<List<Track>> = imported.observeAll().map { list -> list.mapNotNull { Track.fromJson(it.trackJson) } }

    /** Imports audio files the user picked with the system file picker. Returns how many were added. */
    suspend fun import(uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        var added = 0
        uris.forEach { uri ->
            runCatching {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val track = describe(uri)
                imported.upsert(ImportedEntity(uri.toString(), track.toJson(), System.currentTimeMillis()))
                added++
            }
        }
        added
    }

    suspend fun removeImported(track: Track) = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.releasePersistableUriPermission(Uri.parse(track.streamUrl), Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        imported.delete(track.streamUrl)
    }

    private fun describe(uri: Uri): Track {
        var name = "Audio file"
        var size = 0L
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                name = c.getString(0) ?: name
                size = c.getLong(1)
            }
        }
        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var duration = 0L
        val mmr = MediaMetadataRetriever()
        try {
            mmr.setDataSource(context, uri)
            title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
        } finally {
            runCatching { mmr.release() }
        }
        return Track(
            id = "local:$uri",
            title = title?.ifBlank { null } ?: name.substringBeforeLast('.'),
            artist = artist?.ifBlank { null } ?: "Unknown artist",
            album = album?.ifBlank { null } ?: "On this device",
            durationMs = duration,
            streamUrl = uri.toString(),
            artworkUrl = null,
            sourceType = SourceType.LOCAL,
            sourcePageUrl = null,
            license = LicenseInfo(
                name = "Your own file",
                url = null,
                attribution = "Imported from your device.",
                offlineAllowed = false,
                offlineNote = "This file already lives on your phone. You're responsible for having the rights to it.",
            ),
            sizeBytes = size,
        )
    }
}
