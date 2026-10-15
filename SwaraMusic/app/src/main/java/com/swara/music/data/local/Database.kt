package com.swara.music.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

object DownloadStatus {
    const val QUEUED = 0
    const val RUNNING = 1
    const val PAUSED = 2
    const val DONE = 3
    const val FAILED = 4
}

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val trackId: String,
    val downloadId: Long,
    val status: Int,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val localPath: String?,
    val createdAt: Long,
    val trackJson: String,
    val error: String? = null,
) {
    val progress: Float get() = if (totalBytes > 0) (bytesDownloaded.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
    val isActive: Boolean get() = status == DownloadStatus.QUEUED || status == DownloadStatus.RUNNING || status == DownloadStatus.PAUSED
}

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

@Entity(tableName = "playlist_items", indices = [Index("playlistId")])
data class PlaylistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val trackId: String,
    val trackJson: String,
    val addedAt: Long,
)

@Entity(tableName = "imported")
data class ImportedEntity(
    @PrimaryKey val uri: String,
    val trackJson: String,
    val addedAt: Long,
)

data class PlaylistWithCount(val id: Long, val name: String, val createdAt: Long, val count: Int)

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT COUNT(*) FROM downloads WHERE status IN (0,1,2)")
    fun observeActiveCount(): Flow<Int>

    @Query("SELECT * FROM downloads WHERE status IN (0,1,2)")
    suspend fun active(): List<DownloadEntity>

    @Query("SELECT * FROM downloads WHERE trackId = :trackId")
    suspend fun get(trackId: String): DownloadEntity?

    @Query("SELECT * FROM downloads")
    suspend fun all(): List<DownloadEntity>

    @Upsert
    suspend fun upsert(entity: DownloadEntity)

    @Query("DELETE FROM downloads WHERE trackId = :trackId")
    suspend fun delete(trackId: String)

    @Query("DELETE FROM downloads")
    suspend fun deleteAll()
}

@Dao
interface PlaylistDao {
    @Query(
        "SELECT p.id AS id, p.name AS name, p.createdAt AS createdAt, " +
            "(SELECT COUNT(*) FROM playlist_items i WHERE i.playlistId = p.id) AS count " +
            "FROM playlists p ORDER BY p.createdAt DESC"
    )
    fun observePlaylists(): Flow<List<PlaylistWithCount>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun playlist(id: Long): PlaylistEntity?

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY addedAt ASC")
    fun observeItems(playlistId: Long): Flow<List<PlaylistItemEntity>>

    @Insert
    suspend fun insertPlaylist(p: PlaylistEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: PlaylistItemEntity)

    @Query("SELECT COUNT(*) FROM playlist_items WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun contains(playlistId: Long, trackId: String): Int

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeItem(playlistId: Long, trackId: String)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun clearItems(playlistId: Long)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylistRow(playlistId: Long)
}

@Dao
interface ImportedDao {
    @Query("SELECT * FROM imported ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<ImportedEntity>>

    @Upsert
    suspend fun upsert(e: ImportedEntity)

    @Query("DELETE FROM imported WHERE uri = :uri")
    suspend fun delete(uri: String)
}

@Database(
    entities = [DownloadEntity::class, PlaylistEntity::class, PlaylistItemEntity::class, ImportedEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloads(): DownloadDao
    abstract fun playlists(): PlaylistDao
    abstract fun imported(): ImportedDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "swara.db").build()
    }
}
