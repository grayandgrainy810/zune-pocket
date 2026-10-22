package com.zunepocket.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zunepocket.data.Album
import com.zunepocket.data.Artist
import com.zunepocket.data.Genre
import com.zunepocket.data.MusicLibrary
import com.zunepocket.data.Playlist
import com.zunepocket.data.Song
import com.zunepocket.data.jumpLetter
import com.zunepocket.input.ActionHandlerEffect
import com.zunepocket.input.AppAction
import com.zunepocket.nav.LibraryTab
import com.zunepocket.nav.Screen
import com.zunepocket.ui.AppState
import com.zunepocket.ui.LocalAppState
import com.zunepocket.ui.MenuItem
import com.zunepocket.ui.components.AlbumArt
import com.zunepocket.ui.components.EmptyMessage
import com.zunepocket.ui.components.PanoramaTitle
import com.zunepocket.ui.components.PivotHeader
import com.zunepocket.ui.components.ZuneList
import com.zunepocket.ui.components.ZuneListRow
import com.zunepocket.ui.components.pivotSwipe
import com.zunepocket.ui.selection.ListSelection
import com.zunepocket.ui.selection.SelectionMath

/** "music": artists / albums / songs / genres / playlists pivots. */
@Composable
fun LibraryScreen(screen: Screen.Library, library: MusicLibrary) {
    val app = LocalAppState.current
    val tab = LibraryTab.entries[screen.pivot.index]
    val selection = screen.selections[screen.pivot.index]
    val size = tabSize(tab, library)
    val letterAt = letterLookup(tab, library)

    ActionHandlerEffect { action ->
        when (action) {
            AppAction.Left -> screen.pivot.previous()
            AppAction.Right -> screen.pivot.next()
            AppAction.Confirm -> activate(app, tab, library, selection.index)
            AppAction.Context -> showMenu(app, tab, library, selection.index)
            AppAction.JumpList -> {
                if (letterAt == null || size == 0) return@ActionHandlerEffect true
                val available = (0 until size).map(letterAt).toSet()
                app.showJumpList(available, letterAt(selection.index.coerceIn(0, size - 1))) { letter ->
                    selection.index = SelectionMath.indexForLetter(letter, size, letterAt)
                }
            }
            else -> return@ActionHandlerEffect selection.handle(action, size, letterAt)
        }
        true
    }

    Column(Modifier.fillMaxSize().pivotSwipe(screen.pivot)) {
        PanoramaTitle("music", screen.pivot)
        PivotHeader(LibraryTab.entries.map { it.title }, screen.pivot)
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                (slideInHorizontally(tween(260)) { it / 5 * screen.pivot.direction } + fadeIn(tween(220))) togetherWith fadeOut(tween(120))
            },
            label = "libraryTab",
        ) { shownTab ->
            val shownSelection = screen.selections[shownTab.ordinal]
            val onActivate = { i: Int -> activate(app, shownTab, library, i) }
            when (shownTab) {
                LibraryTab.Artists -> TabList(library.artists, shownSelection, onActivate, Artist::id) { artist, selected ->
                    ZuneListRow(artist.name, selected)
                }
                LibraryTab.Albums -> TabList(library.albums, shownSelection, onActivate, Album::id) { album, selected ->
                    ZuneListRow(
                        album.title,
                        selected,
                        secondary = album.artistName,
                        leading = { AlbumArt(album.artSeed, Modifier.size(56.dp)) },
                    )
                }
                LibraryTab.Songs -> TabList(library.songs, shownSelection, onActivate, Song::id) { song, selected ->
                    ZuneListRow(song.title, selected, secondary = song.artistName)
                }
                LibraryTab.Genres -> TabList(library.genres, shownSelection, onActivate, Genre::name) { genre, selected ->
                    ZuneListRow(genre.name.lowercase(), selected, secondary = "${genre.songCount} songs")
                }
                LibraryTab.Playlists -> TabList(library.playlists, shownSelection, onActivate, Playlist::id) { playlist, selected ->
                    ZuneListRow(playlist.name.lowercase(), selected, secondary = "${playlist.songIds.size} songs")
                }
            }
        }
    }
}

@Composable
private fun <T : Any> TabList(
    items: List<T>,
    selection: ListSelection,
    onActivate: (Int) -> Unit,
    key: (T) -> Any,
    row: @Composable (T, Boolean) -> Unit,
) {
    if (items.isEmpty()) {
        EmptyMessage("nothing here yet")
    } else {
        ZuneList(items, selection, onActivate, Modifier.fillMaxSize(), key = key, itemContent = row)
    }
}

private fun tabSize(tab: LibraryTab, library: MusicLibrary): Int = when (tab) {
    LibraryTab.Artists -> library.artists.size
    LibraryTab.Albums -> library.albums.size
    LibraryTab.Songs -> library.songs.size
    LibraryTab.Genres -> library.genres.size
    LibraryTab.Playlists -> library.playlists.size
}

private fun letterLookup(tab: LibraryTab, library: MusicLibrary): ((Int) -> Char)? = when (tab) {
    LibraryTab.Artists -> { i -> jumpLetter(library.artists[i].name) }
    LibraryTab.Albums -> { i -> jumpLetter(library.albums[i].title) }
    LibraryTab.Songs -> { i -> jumpLetter(library.songs[i].title) }
    LibraryTab.Genres -> { i -> jumpLetter(library.genres[i].name) }
    LibraryTab.Playlists -> null
}

private fun activate(app: AppState, tab: LibraryTab, library: MusicLibrary, index: Int) {
    when (tab) {
        LibraryTab.Artists -> library.artists.getOrNull(index)?.let { app.open(Screen.ArtistDetail(it.id)) }
        LibraryTab.Albums -> library.albums.getOrNull(index)?.let { app.open(Screen.AlbumDetail(it.id)) }
        LibraryTab.Songs -> if (index in library.songs.indices) app.play(library.songs, index)
        LibraryTab.Genres -> library.genres.getOrNull(index)?.let { genre ->
            app.open(Screen.SongList(genre.name) { it.songsIn(genre.name) })
        }
        LibraryTab.Playlists -> library.playlists.getOrNull(index)?.let { playlist ->
            app.open(Screen.SongList(playlist.name) { it.songsIn(playlist) })
        }
    }
}

private fun showMenu(app: AppState, tab: LibraryTab, library: MusicLibrary, index: Int) {
    when (tab) {
        LibraryTab.Artists -> library.artists.getOrNull(index)?.let { app.showArtistMenu(it, library) }
        LibraryTab.Albums -> library.albums.getOrNull(index)?.let { app.showAlbumMenu(it, library) }
        LibraryTab.Songs -> library.songs.getOrNull(index)?.let { app.showSongMenu(it, library) }
        LibraryTab.Genres -> library.genres.getOrNull(index)?.let { genre ->
            val songs = library.songsIn(genre.name)
            app.showMenu(
                genre.name,
                listOf(
                    MenuItem("play all") { app.play(songs) },
                    MenuItem("shuffle") { app.shuffleAll(songs) },
                ),
            )
        }
        LibraryTab.Playlists -> library.playlists.getOrNull(index)?.let { playlist ->
            val songs = library.songsIn(playlist)
            app.showMenu(
                playlist.name,
                listOf(
                    MenuItem("play") { app.play(songs) },
                    MenuItem("shuffle") { app.shuffleAll(songs) },
                ),
            )
        }
    }
}
