package com.zunepocket.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zunepocket.AppContainer
import com.zunepocket.data.Album
import com.zunepocket.data.Artist
import com.zunepocket.data.MusicLibrary
import com.zunepocket.data.Song
import com.zunepocket.nav.Navigator
import com.zunepocket.nav.Screen
import com.zunepocket.player.Repeat
import com.zunepocket.ui.selection.ListSelection

data class MenuItem(val label: String, val action: () -> Unit)

/** Modal layers drawn above the current screen. */
sealed interface Overlay {
    class Menu(val title: String, val items: List<MenuItem>) : Overlay {
        val selection = ListSelection()
    }

    class JumpList(val available: Set<Char>, val initial: Char, val onPick: (Char) -> Unit) : Overlay
}

val LocalAppState = compositionLocalOf<AppState> { error("No AppState provided") }

/** App-wide UI state and the shared actions screens use (navigate, play, show menus). */
@Stable
class AppState(val container: AppContainer) {
    val navigator = Navigator(Screen.Home())
    val player get() = container.player
    val settings get() = container.settings

    var overlay by mutableStateOf<Overlay?>(null)
        private set

    fun open(screen: Screen) = navigator.push(screen)

    fun openNowPlaying() {
        if (player.state.value.current == null) return
        if (navigator.current !is Screen.NowPlaying) open(Screen.NowPlaying())
    }

    fun openQueue() {
        open(Screen.Queue(player.state.value.index.coerceAtLeast(0)))
    }

    fun play(songs: List<Song>, index: Int = 0, showNowPlaying: Boolean = true) {
        if (songs.isEmpty()) return
        player.play(songs, index)
        if (showNowPlaying) openNowPlaying()
    }

    fun shuffleAll(songs: List<Song>) {
        if (songs.isEmpty()) return
        val shuffled = songs.shuffled()
        if (!player.state.value.shuffle) player.toggleShuffle()
        play(shuffled, 0)
    }

    fun showMenu(title: String, items: List<MenuItem>) {
        overlay = Overlay.Menu(title, items)
    }

    fun showJumpList(available: Set<Char>, initial: Char, onPick: (Char) -> Unit) {
        overlay = Overlay.JumpList(available, initial, onPick)
    }

    fun dismissOverlay() {
        overlay = null
    }

    // Standard context menus, shared by every screen that lists these items.

    fun showSongMenu(song: Song, library: MusicLibrary) = showMenu(
        song.title,
        listOf(
            MenuItem("play") { play(listOf(song)) },
            MenuItem("play next") { player.playNext(listOf(song)) },
            MenuItem("add to now playing") { player.addToQueue(listOf(song)) },
            MenuItem("go to album") { open(Screen.AlbumDetail(song.albumId)) },
            MenuItem("go to artist") { open(Screen.ArtistDetail(song.artistId)) },
        ),
    )

    fun showAlbumMenu(album: Album, library: MusicLibrary) {
        val songs = library.songsOn(album.id)
        showMenu(
            album.title,
            listOf(
                MenuItem("play") { play(songs) },
                MenuItem("shuffle") { shuffleAll(songs) },
                MenuItem("play next") { player.playNext(songs) },
                MenuItem("add to now playing") { player.addToQueue(songs) },
                MenuItem("go to artist") { open(Screen.ArtistDetail(album.artistId)) },
            ),
        )
    }

    fun showArtistMenu(artist: Artist, library: MusicLibrary) {
        val songs = library.songsBy(artist.id)
        showMenu(
            artist.name,
            listOf(
                MenuItem("play all") { play(songs) },
                MenuItem("shuffle") { shuffleAll(songs) },
                MenuItem("add to now playing") { player.addToQueue(songs) },
            ),
        )
    }

    fun showNowPlayingMenu() {
        val state = player.state.value
        val song = state.current ?: return
        val repeatLabel = when (state.repeat) {
            Repeat.Off -> "repeat: off"
            Repeat.All -> "repeat: all"
            Repeat.One -> "repeat: one"
        }
        showMenu(
            song.title,
            listOf(
                MenuItem(if (state.shuffle) "shuffle: on" else "shuffle: off") { player.toggleShuffle() },
                MenuItem(repeatLabel) { player.cycleRepeat() },
                MenuItem("view queue") { openQueue() },
                MenuItem("go to album") { open(Screen.AlbumDetail(song.albumId)) },
                MenuItem("go to artist") { open(Screen.ArtistDetail(song.artistId)) },
            ),
        )
    }
}
