package com.zunepocket.player

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.zunepocket.data.MusicLibrary
import com.zunepocket.data.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Real playback through [PlaybackService]. Our own [queue] is the source of truth (so the queue screen
 * shows the shuffled order), mirrored onto the player's media items. Commands issued before the
 * service connects are held and replayed.
 */
class Media3PlayerController(
    private val context: Context,
    scope: CoroutineScope,
    private val prefs: SharedPreferences,
) : PlayerController {

    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private val pending = ArrayDeque<(MediaController) -> Unit>()

    private var queue: List<Song> = emptyList()
    /** Queue order before shuffling, so turning shuffle off restores it. */
    private var originalQueue: List<Song> = emptyList()
    private var shuffle = prefs.getBoolean(KEY_SHUFFLE, false)
    private var restored = false

    init {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            val connected = runCatching { future.get() }.getOrNull() ?: return@addListener
            controller = connected
            connected.addListener(object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    sync()
                    if (events.containsAny(Player.EVENT_MEDIA_ITEM_TRANSITION, Player.EVENT_IS_PLAYING_CHANGED)) save()
                }
            })
            while (pending.isNotEmpty()) pending.removeFirst().invoke(connected)
            sync()
        }, ContextCompat.getMainExecutor(context))

        // Position updates while playing, and a periodic save so a crash loses little.
        scope.launch {
            var ticks = 0
            while (isActive) {
                delay(POLL_MS)
                if (controller?.isPlaying == true) {
                    sync()
                    if (++ticks % SAVE_EVERY_TICKS == 0) save()
                }
            }
        }
    }

    override fun play(songs: List<Song>, startIndex: Int) {
        if (songs.isEmpty()) return
        val start = startIndex.coerceIn(songs.indices)
        originalQueue = songs
        val index: Int
        if (shuffle) {
            queue = listOf(songs[start]) + songs.filterIndexed { i, _ -> i != start }.shuffled()
            index = 0
        } else {
            queue = songs
            index = start
        }
        val items = queue.map(::toMediaItem)
        withPlayer {
            it.setMediaItems(items, index, 0)
            it.prepare()
            it.play()
        }
        publish(index, playing = true, positionMs = 0)
    }

    override fun toggle() {
        if (queue.isEmpty()) return
        withPlayer {
            when {
                it.playbackState == Player.STATE_ENDED -> {
                    it.seekTo(it.currentMediaItemIndex, 0)
                    it.play()
                }
                it.playWhenReady -> it.pause()
                else -> {
                    if (it.playbackState == Player.STATE_IDLE) it.prepare()
                    it.play()
                }
            }
        }
    }

    override fun pause() = withPlayer { it.pause() }

    override fun next() = withPlayer { if (it.hasNextMediaItem()) it.seekToNextMediaItem() }

    override fun previous() = withPlayer {
        if (it.currentPosition > RESTART_THRESHOLD_MS || !it.hasPreviousMediaItem()) it.seekTo(0) else it.seekToPreviousMediaItem()
    }

    override fun seekBy(deltaMs: Long) = withPlayer {
        val duration = it.duration.takeIf { d -> d != C.TIME_UNSET } ?: Long.MAX_VALUE
        it.seekTo((it.currentPosition + deltaMs).coerceIn(0, duration))
    }

    override fun seekTo(positionMs: Long) = withPlayer { it.seekTo(positionMs.coerceAtLeast(0)) }

    override fun jumpTo(index: Int) {
        if (index !in queue.indices) return
        withPlayer {
            it.seekTo(index, 0)
            it.play()
        }
    }

    override fun toggleShuffle() {
        shuffle = !shuffle
        val currentIndex = _state.value.index
        val current = queue.getOrNull(currentIndex)
        if (current == null) {
            sync()
            return
        }
        val newQueue: List<Song>
        val newIndex: Int
        if (shuffle) {
            newQueue = listOf(current) + queue.filterIndexed { i, _ -> i != currentIndex }.shuffled()
            newIndex = 0
        } else {
            newQueue = originalQueue.ifEmpty { queue }
            newIndex = newQueue.indexOf(current).coerceAtLeast(0)
        }
        replaceAroundCurrent(newQueue, newIndex)
    }

    override fun cycleRepeat() = withPlayer {
        it.repeatMode = when (it.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    override fun playNext(songs: List<Song>) {
        if (songs.isEmpty()) return
        val index = _state.value.index
        if (queue.isEmpty() || index < 0) return play(songs)
        queue = queue.toMutableList().apply { addAll(index + 1, songs) }
        val originalAt = (originalQueue.indexOf(queue[index]) + 1).coerceIn(0, originalQueue.size)
        originalQueue = originalQueue.toMutableList().apply { addAll(originalAt, songs) }
        val items = songs.map(::toMediaItem)
        withPlayer { it.addMediaItems(it.currentMediaItemIndex + 1, items) }
        sync()
    }

    override fun addToQueue(songs: List<Song>) {
        if (songs.isEmpty()) return
        if (queue.isEmpty()) return play(songs)
        queue = queue + songs
        originalQueue = originalQueue + songs
        val items = songs.map(::toMediaItem)
        withPlayer { it.addMediaItems(items) }
        sync()
    }

    override fun removeFromQueue(index: Int) {
        if (index !in queue.indices || index == _state.value.index) return
        val removed = queue[index]
        queue = queue.toMutableList().apply { removeAt(index) }
        val originalIndex = originalQueue.indexOf(removed)
        if (originalIndex >= 0) originalQueue = originalQueue.toMutableList().apply { removeAt(originalIndex) }
        withPlayer { it.removeMediaItem(index) }
        sync()
    }

    /** Restores the last session's queue (paused) once the library is loaded. Runs once. */
    fun restore(library: MusicLibrary) {
        if (restored) return
        restored = true
        if (queue.isNotEmpty()) return
        val ids = prefs.getString(KEY_QUEUE, null)?.split(',')?.mapNotNull { it.toLongOrNull() }.orEmpty()
        val songs = ids.mapNotNull { library.song(it) }
        if (songs.isEmpty()) return
        val index = prefs.getInt(KEY_INDEX, 0).coerceIn(songs.indices)
        val position = prefs.getLong(KEY_POSITION, 0)
        queue = songs
        originalQueue = songs
        val items = songs.map(::toMediaItem)
        withPlayer {
            if (it.mediaItemCount == 0) {
                it.setMediaItems(items, index, position)
                it.prepare()
            }
        }
        publish(index, playing = false, positionMs = position)
    }

    /** Swaps every item except the playing one, so playback continues uninterrupted. */
    private fun replaceAroundCurrent(newQueue: List<Song>, newIndex: Int) {
        queue = newQueue
        val before = newQueue.subList(0, newIndex).map(::toMediaItem)
        val after = newQueue.subList(newIndex + 1, newQueue.size).map(::toMediaItem)
        withPlayer {
            val current = it.currentMediaItemIndex
            it.removeMediaItems(current + 1, it.mediaItemCount)
            it.removeMediaItems(0, current)
            it.addMediaItems(0, before)
            it.addMediaItems(after)
        }
        sync()
        save()
    }

    private inline fun withPlayer(crossinline block: (MediaController) -> Unit) {
        val c = controller
        if (c != null) block(c) else pending.addLast { block(it) }
    }

    /** Publishes state from the player, or from our own values before the service has connected. */
    private fun sync() {
        val c = controller ?: return publish(_state.value.index, _state.value.isPlaying, _state.value.positionMs)
        val index = if (queue.isEmpty()) -1 else c.currentMediaItemIndex.coerceIn(0, queue.lastIndex)
        _state.value = PlaybackState(
            queue = queue,
            index = index,
            isPlaying = c.playWhenReady && c.playbackState != Player.STATE_ENDED,
            positionMs = c.currentPosition.coerceAtLeast(0),
            shuffle = shuffle,
            repeat = when (c.repeatMode) {
                Player.REPEAT_MODE_ONE -> Repeat.One
                Player.REPEAT_MODE_ALL -> Repeat.All
                else -> Repeat.Off
            },
        )
    }

    private fun publish(index: Int, playing: Boolean, positionMs: Long) {
        if (controller != null) return sync()
        _state.value = _state.value.copy(queue = queue, index = index, isPlaying = playing, positionMs = positionMs, shuffle = shuffle)
    }

    private fun save() {
        val s = _state.value
        prefs.edit()
            .putString(KEY_QUEUE, queue.joinToString(",") { it.id.toString() })
            .putInt(KEY_INDEX, s.index.coerceAtLeast(0))
            .putLong(KEY_POSITION, s.positionMs)
            .putBoolean(KEY_SHUFFLE, shuffle)
            .apply()
    }

    private fun toMediaItem(song: Song): MediaItem = MediaItem.Builder()
        .setMediaId(song.id.toString())
        .setUri(song.uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artistName)
                .setAlbumTitle(song.albumTitle)
                .build(),
        )
        .build()

    private companion object {
        const val POLL_MS = 250L
        const val SAVE_EVERY_TICKS = 40 // 10 s
        const val RESTART_THRESHOLD_MS = 3_000L
        const val KEY_QUEUE = "queue_ids"
        const val KEY_INDEX = "queue_index"
        const val KEY_POSITION = "queue_position"
        const val KEY_SHUFFLE = "shuffle"
    }
}
