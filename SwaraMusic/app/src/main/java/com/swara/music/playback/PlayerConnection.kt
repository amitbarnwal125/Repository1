package com.swara.music.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.swara.music.data.NetworkMonitor
import com.swara.music.data.model.SourceType
import com.swara.music.data.model.Track
import com.swara.music.data.repo.DownloadRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerState(
    val current: Track? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
)

/** App-side handle to the playback service. All UI talks to the player through here. */
class PlayerConnection(
    private val context: Context,
    private val scope: CoroutineScope,
    private val downloads: DownloadRepository,
    private val network: NetworkMonitor,
) {
    private var future: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private val pending = mutableListOf<(MediaController) -> Unit>()
    private var ticker: Job? = null

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun message(text: String) { _messages.tryEmit(text) }

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = refresh()
        override fun onPlayerError(error: PlaybackException) {
            val msg = when (error.errorCode) {
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                    "Couldn't reach the music server. Check your connection."
                PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS, PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                    "This track is no longer available from its source."
                PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
                PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ->
                    "This audio format isn't supported on your phone."
                else -> "Playback error: ${error.errorCodeName}"
            }
            message(msg)
        }
    }

    fun connect() {
        if (future != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val f = MediaController.Builder(context, token).buildAsync()
        future = f
        f.addListener({
            val c = runCatching { f.get() }.getOrNull() ?: run { future = null; return@addListener }
            controller = c
            c.addListener(listener)
            refresh()
            pending.forEach { it(c) }
            pending.clear()
        }, ContextCompat.getMainExecutor(context))
    }

    private fun withController(block: (MediaController) -> Unit) {
        val c = controller
        if (c != null) block(c) else {
            pending += block
            connect()
        }
    }

    // ---- Commands -------------------------------------------------------------------------

    /** Replaces the queue with [tracks] and starts at [startIndex]. */
    fun play(tracks: List<Track>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        scope.launch {
            val resolved = tracks.map { it to resolveUri(it) }
            val start = resolved.getOrNull(startIndex)
            if (start == null || start.second == null) {
                message(offlineMessage())
                return@launch
            }
            // When offline, keep only tracks that can actually play.
            val playable = resolved.filter { it.second != null }
            val newStart = playable.indexOfFirst { it.first.id == start.first.id }.coerceAtLeast(0)
            val items = playable.map { (t, uri) -> toMediaItem(t, uri!!) }
            withController { c ->
                c.setMediaItems(items, newStart, 0L)
                c.prepare()
                c.play()
            }
        }
    }

    fun playNext(track: Track) = insert(track, next = true)
    fun addToQueue(track: Track) = insert(track, next = false)

    private fun insert(track: Track, next: Boolean) {
        scope.launch {
            val uri = resolveUri(track) ?: run { message(offlineMessage()); return@launch }
            withController { c ->
                val item = toMediaItem(track, uri)
                if (c.mediaItemCount == 0) {
                    c.setMediaItem(item); c.prepare(); c.play()
                } else if (next) {
                    c.addMediaItem(c.currentMediaItemIndex + 1, item)
                } else {
                    c.addMediaItem(item)
                }
                message(if (next) "Playing next: ${track.title}" else "Added to queue: ${track.title}")
            }
        }
    }

    fun togglePlay() = withController { c ->
        if (c.isPlaying) c.pause() else {
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            if (c.playbackState == Player.STATE_ENDED) c.seekToDefaultPosition()
            c.play()
        }
    }

    fun next() = withController { if (it.hasNextMediaItem()) it.seekToNextMediaItem() }
    fun previous() = withController { it.seekToPrevious() }
    fun seekTo(ms: Long) = withController { it.seekTo(ms); refresh() }
    fun skipToIndex(index: Int) = withController { it.seekToDefaultPosition(index); it.play() }
    fun removeAt(index: Int) = withController { it.removeMediaItem(index) }
    fun move(from: Int, to: Int) = withController { it.moveMediaItem(from, to) }
    fun toggleShuffle() = withController { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    fun cycleRepeat() = withController {
        it.repeatMode = when (it.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }
    fun clearQueue() = withController { it.stop(); it.clearMediaItems() }

    // ---- Helpers --------------------------------------------------------------------------

    private fun offlineMessage() =
        if (network.isOnline) "This track can't be played right now."
        else "You're offline. Download songs to listen without internet."

    /** Downloaded file > device file > stream (only when online). */
    private suspend fun resolveUri(track: Track): Uri? {
        if (track.sourceType == SourceType.LOCAL) return Uri.parse(track.streamUrl)
        downloads.localFile(track.id)?.let { return Uri.fromFile(it) }
        return if (network.isOnline) Uri.parse(track.streamUrl) else null
    }

    private fun toMediaItem(track: Track, uri: Uri): MediaItem = MediaItem.Builder()
        .setMediaId(track.id)
        .setUri(uri)
        .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(track.title)
                .setArtist(track.artist)
                .setAlbumTitle(track.album)
                .setArtworkUri(track.artworkUrl?.let(Uri::parse))
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .setExtras(bundleOf(EXTRA_TRACK to track.toJson()))
                .build()
        )
        .build()

    private fun trackOf(item: MediaItem?): Track? =
        item?.mediaMetadata?.extras?.getString(EXTRA_TRACK)?.let(Track::fromJson)

    private fun refresh() {
        val c = controller ?: return
        val queue = (0 until c.mediaItemCount).mapNotNull { trackOf(c.getMediaItemAt(it)) }
        _state.value = PlayerState(
            current = trackOf(c.currentMediaItem),
            isPlaying = c.isPlaying,
            isBuffering = c.playbackState == Player.STATE_BUFFERING,
            positionMs = c.currentPosition.coerceAtLeast(0),
            durationMs = c.duration.takeIf { it > 0 } ?: (trackOf(c.currentMediaItem)?.durationMs ?: 0),
            queue = queue,
            currentIndex = c.currentMediaItemIndex,
            shuffle = c.shuffleModeEnabled,
            repeatMode = c.repeatMode,
            hasNext = c.hasNextMediaItem(),
            hasPrevious = c.hasPreviousMediaItem() || c.currentPosition > 3000,
        )
        if (c.isPlaying && ticker?.isActive != true) {
            ticker = scope.launch {
                while (isActive) {
                    delay(500)
                    val cc = controller ?: break
                    _state.value = _state.value.copy(
                        positionMs = cc.currentPosition.coerceAtLeast(0),
                        durationMs = cc.duration.takeIf { it > 0 } ?: _state.value.durationMs,
                    )
                    if (!cc.isPlaying) break
                }
            }
        }
    }

    companion object {
        const val EXTRA_TRACK = "swara_track"
    }
}
