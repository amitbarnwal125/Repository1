package com.swara.music.data.repo

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import com.swara.music.data.Settings
import com.swara.music.data.local.AppDatabase
import com.swara.music.data.local.DownloadEntity
import com.swara.music.data.local.DownloadStatus
import com.swara.music.data.model.Track
import com.swara.music.data.remote.Http
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * In-app downloads using Android's system DownloadManager. Files go to the app's
 * private music folder (Android/data/<app>/files/Music), so no storage permission
 * is needed and everything is removed when the app is uninstalled.
 *
 * A download is only ever started for tracks whose licence AND provider terms
 * permit offline copies ([Track.canDownload]).
 */
class DownloadRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val settings: Settings,
    private val scope: CoroutineScope,
) {
    private val dm = context.getSystemService(DownloadManager::class.java)
    private val dao = db.downloads()

    val downloads: StateFlow<List<DownloadEntity>> =
        dao.observeAll().stateIn(scope, SharingStarted.Eagerly, emptyList())

    val byTrackId: StateFlow<Map<String, DownloadEntity>> =
        downloads.map { list -> list.associateBy { it.trackId } }.stateIn(scope, SharingStarted.Eagerly, emptyMap())

    val completedTracks: Flow<List<Track>> = downloads.map { list ->
        list.filter { it.status == DownloadStatus.DONE }.mapNotNull { Track.fromJson(it.trackJson) }
    }

    private val musicDir: File
        get() = (context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: File(context.filesDir, "music")).apply { mkdirs() }

    init {
        // Poll DownloadManager while something is in flight to keep progress live.
        scope.launch {
            dao.observeActiveCount().distinctUntilChanged().collectLatest { active ->
                if (active > 0) {
                    while (isActive) {
                        sync()
                        delay(600)
                    }
                }
            }
        }
        scope.launch { sync() }
    }

    sealed interface StartResult {
        data object Started : StartResult
        data object AlreadyDownloaded : StartResult
        data class NotAllowed(val reason: String) : StartResult
        data class Error(val message: String) : StartResult
    }

    suspend fun start(track: Track): StartResult = withContext(Dispatchers.IO) {
        if (!track.canDownload) return@withContext StartResult.NotAllowed(track.license.offlineNote)
        val existing = dao.get(track.id)
        if (existing != null && existing.status == DownloadStatus.DONE && existing.localPath?.let { File(it).exists() } == true) {
            return@withContext StartResult.AlreadyDownloaded
        }
        if (existing != null && existing.isActive) return@withContext StartResult.Started
        existing?.let { runCatching { dm.remove(it.downloadId) } }

        val ext = track.streamUrl.substringAfterLast('/').substringAfterLast('.', "audio").take(5).lowercase()
        val fileName = sha1(track.id).take(20) + "." + ext
        File(musicDir, fileName).delete()
        try {
            val request = DownloadManager.Request(Uri.parse(track.streamUrl))
                .addRequestHeader("User-Agent", Http.USER_AGENT)
                .setTitle(track.title)
                .setDescription("Swara · ${track.artist}")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_MUSIC, fileName)
                .setAllowedOverMetered(!settings.wifiOnly.value)
                .setAllowedOverRoaming(false)
            val id = dm.enqueue(request)
            dao.upsert(
                DownloadEntity(
                    trackId = track.id,
                    downloadId = id,
                    status = DownloadStatus.QUEUED,
                    bytesDownloaded = 0,
                    totalBytes = track.sizeBytes,
                    localPath = File(musicDir, fileName).absolutePath,
                    createdAt = System.currentTimeMillis(),
                    trackJson = track.toJson(),
                )
            )
            StartResult.Started
        } catch (e: Exception) {
            StartResult.Error(e.message ?: "Couldn't start the download")
        }
    }

    suspend fun cancel(trackId: String) = withContext(Dispatchers.IO) {
        dao.get(trackId)?.let { e ->
            runCatching { dm.remove(e.downloadId) }
            e.localPath?.let { File(it).delete() }
        }
        dao.delete(trackId)
    }

    suspend fun delete(trackId: String) = cancel(trackId)

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        dao.all().forEach { e ->
            runCatching { dm.remove(e.downloadId) }
            e.localPath?.let { File(it).delete() }
        }
        musicDir.listFiles()?.forEach { it.delete() }
        dao.deleteAll()
    }

    /** Local file for a fully downloaded track, or null. */
    suspend fun localFile(trackId: String): File? = withContext(Dispatchers.IO) {
        val e = dao.get(trackId) ?: return@withContext null
        if (e.status != DownloadStatus.DONE) return@withContext null
        e.localPath?.let(::File)?.takeIf { it.exists() && it.length() > 0 }
    }

    data class StorageInfo(val usedBytes: Long, val freeBytes: Long, val fileCount: Int)

    suspend fun storageInfo(): StorageInfo = withContext(Dispatchers.IO) {
        val files = musicDir.listFiles()?.filter { it.isFile } ?: emptyList()
        val free = runCatching { StatFs(musicDir.path).availableBytes }.getOrDefault(0L)
        StorageInfo(files.sumOf { it.length() }, free, files.size)
    }

    private suspend fun sync() = withContext(Dispatchers.IO) {
        val active = dao.active()
        if (active.isEmpty()) return@withContext
        val ids = active.map { it.downloadId }.toLongArray()
        val seen = mutableSetOf<Long>()
        val queried = runCatching {
            dm.query(DownloadManager.Query().setFilterById(*ids))?.use { c ->
                val idCol = c.getColumnIndexOrThrow(DownloadManager.COLUMN_ID)
                val statusCol = c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                val soFarCol = c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val totalCol = c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                val reasonCol = c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)
                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    seen += id
                    val entity = active.firstOrNull { it.downloadId == id }
                    if (entity != null) {
                    val dmStatus = c.getInt(statusCol)
                    val soFar = c.getLong(soFarCol)
                    val total = c.getLong(totalCol).takeIf { it > 0 } ?: entity.totalBytes
                    val updated = when (dmStatus) {
                        DownloadManager.STATUS_SUCCESSFUL -> entity.copy(
                            status = DownloadStatus.DONE,
                            bytesDownloaded = total,
                            totalBytes = entity.localPath?.let { File(it).length() }?.takeIf { it > 0 } ?: total,
                        )
                        DownloadManager.STATUS_FAILED -> entity.copy(
                            status = DownloadStatus.FAILED,
                            error = failureText(c.getInt(reasonCol)),
                        )
                        DownloadManager.STATUS_PAUSED -> entity.copy(
                            status = DownloadStatus.PAUSED, bytesDownloaded = soFar, totalBytes = total,
                            error = pauseText(c.getInt(reasonCol)),
                        )
                        DownloadManager.STATUS_RUNNING -> entity.copy(
                            status = DownloadStatus.RUNNING, bytesDownloaded = soFar, totalBytes = total, error = null,
                        )
                        else -> entity.copy(status = DownloadStatus.QUEUED, bytesDownloaded = soFar, totalBytes = total)
                    }
                    if (updated != entity) dao.upsert(updated)
                    }
                }
            } != null
        }.getOrDefault(false)
        // Rows the system no longer knows about were cancelled from the notification.
        if (queried) active.filter { it.downloadId !in seen }.forEach { dao.delete(it.trackId) }
    }

    private fun failureText(reason: Int) = when (reason) {
        DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Not enough storage space"
        DownloadManager.ERROR_HTTP_DATA_ERROR, DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "The server didn't send the file"
        DownloadManager.ERROR_CANNOT_RESUME -> "Download was interrupted"
        else -> "Download failed (code $reason)"
    }

    private fun pauseText(reason: Int) = when (reason) {
        DownloadManager.PAUSED_QUEUED_FOR_WIFI -> "Waiting for Wi-Fi"
        DownloadManager.PAUSED_WAITING_FOR_NETWORK -> "Waiting for network"
        DownloadManager.PAUSED_WAITING_TO_RETRY -> "Retrying soon"
        else -> "Paused"
    }

    private fun sha1(s: String): String =
        MessageDigest.getInstance("SHA-1").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
}
