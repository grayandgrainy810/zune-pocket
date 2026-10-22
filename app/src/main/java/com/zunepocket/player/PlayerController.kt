package com.zunepocket.player

import com.zunepocket.data.Song
import kotlinx.coroutines.flow.StateFlow

/**
 * Playback control used by the UI. The prototype uses [FakePlayerController]; phase 2 swaps in a
 * Media3 implementation behind this interface.
 */
interface PlayerController {
    val state: StateFlow<PlaybackState>

    fun play(songs: List<Song>, startIndex: Int = 0)
    fun toggle()
    fun pause()
    fun next()
    fun previous()
    fun seekBy(deltaMs: Long)
    fun seekTo(positionMs: Long)
    fun jumpTo(index: Int)
    fun toggleShuffle()
    fun cycleRepeat()
    fun playNext(songs: List<Song>)
    fun addToQueue(songs: List<Song>)
    fun removeFromQueue(index: Int)
}
