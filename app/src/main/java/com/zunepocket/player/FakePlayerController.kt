package com.zunepocket.player

import com.zunepocket.data.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Simulates playback with a ticking clock so the UI behaves as it will with real audio. No sound. */
class FakePlayerController(
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) : PlayerController {

    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    /** Queue order before shuffling, so turning shuffle off restores it. */
    private var originalQueue: List<Song> = emptyList()

    init {
        scope.launch {
            while (isActive) {
                delay(TICK_MS)
                val s = _state.value
                val song = s.current
                if (!s.isPlaying || song == null) continue
                val position = s.positionMs + TICK_MS
                if (position >= song.durationMs) onTrackEnded() else _state.value = s.copy(positionMs = position)
            }
        }
    }

    override fun play(songs: List<Song>, startIndex: Int) {
        if (songs.isEmpty()) return
        val start = startIndex.coerceIn(songs.indices)
        originalQueue = songs
        val s = _state.value
        _state.value = if (s.shuffle) {
            val rest = songs.filterIndexed { i, _ -> i != start }.shuffled()
            s.copy(queue = listOf(songs[start]) + rest, index = 0, positionMs = 0, isPlaying = true)
        } else {
            s.copy(queue = songs, index = start, positionMs = 0, isPlaying = true)
        }
    }

    override fun toggle() {
        val s = _state.value
        if (s.current == null) return
        _state.value = s.copy(isPlaying = !s.isPlaying)
    }

    override fun pause() {
        _state.value = _state.value.copy(isPlaying = false)
    }

    override fun next() {
        val s = _state.value
        when {
            s.queue.isEmpty() -> Unit
            s.index < s.queue.lastIndex -> _state.value = s.copy(index = s.index + 1, positionMs = 0)
            s.repeat == Repeat.All -> _state.value = s.copy(index = 0, positionMs = 0)
        }
    }

    override fun previous() {
        val s = _state.value
        if (s.queue.isEmpty()) return
        _state.value = if (s.positionMs > RESTART_THRESHOLD_MS || s.index == 0) {
            s.copy(positionMs = 0)
        } else {
            s.copy(index = s.index - 1, positionMs = 0)
        }
    }

    override fun seekBy(deltaMs: Long) {
        val s = _state.value
        seekTo(s.positionMs + deltaMs)
    }

    override fun seekTo(positionMs: Long) {
        val s = _state.value
        val song = s.current ?: return
        _state.value = s.copy(positionMs = positionMs.coerceIn(0, song.durationMs - 1))
    }

    override fun jumpTo(index: Int) {
        val s = _state.value
        if (index !in s.queue.indices) return
        _state.value = s.copy(index = index, positionMs = 0, isPlaying = true)
    }

    override fun toggleShuffle() {
        val s = _state.value
        val current = s.current
        if (current == null) {
            _state.value = s.copy(shuffle = !s.shuffle)
            return
        }
        _state.value = if (!s.shuffle) {
            val rest = s.queue.filterIndexed { i, _ -> i != s.index }.shuffled()
            s.copy(shuffle = true, queue = listOf(current) + rest, index = 0)
        } else {
            val restored = originalQueue.ifEmpty { s.queue }
            s.copy(shuffle = false, queue = restored, index = restored.indexOf(current).coerceAtLeast(0))
        }
    }

    override fun cycleRepeat() {
        val s = _state.value
        _state.value = s.copy(repeat = Repeat.entries[(s.repeat.ordinal + 1) % Repeat.entries.size])
    }

    override fun playNext(songs: List<Song>) {
        if (songs.isEmpty()) return
        val s = _state.value
        if (s.current == null) return play(songs)
        val at = s.index + 1
        _state.value = s.copy(queue = s.queue.toMutableList().apply { addAll(at, songs) })
        val originalAt = (originalQueue.indexOf(s.current) + 1).coerceIn(0, originalQueue.size)
        originalQueue = originalQueue.toMutableList().apply { addAll(originalAt, songs) }
    }

    override fun addToQueue(songs: List<Song>) {
        if (songs.isEmpty()) return
        val s = _state.value
        if (s.current == null) return play(songs)
        _state.value = s.copy(queue = s.queue + songs)
        originalQueue = originalQueue + songs
    }

    override fun removeFromQueue(index: Int) {
        val s = _state.value
        if (index !in s.queue.indices || index == s.index) return
        val removed = s.queue[index]
        _state.value = s.copy(
            queue = s.queue.toMutableList().apply { removeAt(index) },
            index = if (index < s.index) s.index - 1 else s.index,
        )
        val originalIndex = originalQueue.indexOf(removed)
        if (originalIndex >= 0) originalQueue = originalQueue.toMutableList().apply { removeAt(originalIndex) }
    }

    private fun onTrackEnded() {
        val s = _state.value
        _state.value = when {
            s.repeat == Repeat.One -> s.copy(positionMs = 0)
            s.index < s.queue.lastIndex -> s.copy(index = s.index + 1, positionMs = 0)
            s.repeat == Repeat.All -> s.copy(index = 0, positionMs = 0)
            else -> s.copy(positionMs = 0, isPlaying = false)
        }
    }

    private companion object {
        const val TICK_MS = 250L
        const val RESTART_THRESHOLD_MS = 3_000L
    }
}
