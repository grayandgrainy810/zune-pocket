package com.zunepocket.nav

import com.zunepocket.data.MusicLibrary
import com.zunepocket.data.Song
import com.zunepocket.ui.selection.ListSelection
import com.zunepocket.ui.selection.PivotState

enum class LibraryTab(val title: String) { Artists("artists"), Albums("albums"), Songs("songs"), Genres("genres"), Playlists("playlists") }

/** Back-stack entries. Each one owns its UI state, so returning to a screen restores its selection. */
sealed class Screen {
    class Home : Screen() {
        val selection = ListSelection()
    }

    class Library(initialTab: LibraryTab = LibraryTab.Artists) : Screen() {
        val pivot = PivotState(LibraryTab.entries.size, initialTab.ordinal)
        val selections = List(LibraryTab.entries.size) { ListSelection() }
    }

    class ArtistDetail(val artistId: Long) : Screen() {
        val pivot = PivotState(2)
        val selections = List(2) { ListSelection() }
    }

    class AlbumDetail(val albumId: Long) : Screen() {
        val selection = ListSelection()
    }

    /** A titled list of songs: a genre or a playlist. */
    class SongList(val title: String, val songs: (MusicLibrary) -> List<Song>) : Screen() {
        val selection = ListSelection()
    }

    class NowPlaying : Screen()

    class Queue(initialIndex: Int) : Screen() {
        val selection = ListSelection(initialIndex)
    }

    class Settings : Screen() {
        val selection = ListSelection()
    }

    class KeyTester : Screen()
}
