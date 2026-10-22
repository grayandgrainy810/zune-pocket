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
import com.zunepocket.settings.AlbumView
import com.zunepocket.ui.components.AlbumArt
import com.zunepocket.ui.components.AlbumTile
import com.zunepocket.ui.components.ZuneGrid
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
    val albumView = app.settings.albumView
    // Tile layouts use the grid's own D-pad handling; Left/Right switch pivot only at a row's edge.
    val gridColumns = if (tab == LibraryTab.Albums && albumView != AlbumView.List) albumView.columns else null

    ActionHandlerEffect { action ->
        when (action) {
            AppAction.Left ->
                if (gridColumns == null || selection.handleGrid(action, size, gridColumns) == null) screen.pivot.previous()
            AppAction.Right ->
                if (gridColumns == null || selection.handleGrid(action, size, gridColumns) == null) screen.pivot.next()
            AppAction.Confirm -> activate(app, tab, library, selection.index)
            AppAction.Context -> showMenu(app, tab, library, selection.index)
            AppAction.JumpList -> {
                if (letterAt == null || size == 0) return@ActionHandlerEffect true
                val available = (0 until size).map(letterAt).toSet()
                app.showJumpList(available, letterAt(selection.index.coerceIn(0, size - 1))) { letter ->
                    selection.index = SelectionMath.indexForLetter(letter, size, letterAt)
                }
            }
            else -> return@ActionHandlerEffect if (gridColumns != null) {
                selection.handleGrid(action, size, gridColumns, letterAt) ?: false
            } else {
                selection.handle(action, size, letterAt)
            }
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
            val emptyText = when {
                shownTab == LibraryTab.Playlists && !app.settings.useSampleLibrary -> "playlists are coming soon"
                library.songs.isEmpty() && !app.container.hasAudioPermission -> "allow access to music from the home screen"
                library.songs.isEmpty() -> "no music found. copy songs to your sd card or internal storage"
                else -> "nothing here yet"
            }
            val onActivate = { i: Int -> activate(app, shownTab, library, i) }
            when (shownTab) {
                LibraryTab.Artists -> TabList(library.artists, shownSelection, onActivate, Artist::id, emptyText) { artist, selected ->
                    ZuneListRow(artist.name, selected)
                }
                LibraryTab.Albums -> if (albumView != AlbumView.List && library.albums.isNotEmpty()) {
                    ZuneGrid(library.albums, shownSelection, albumView.columns, onActivate, Modifier.fillMaxSize(), key = Album::id) { album, selected ->
                        AlbumTile(album, selected, showArtist = albumView == AlbumView.LargeTiles)
                    }
                } else TabList(library.albums, shownSelection, onActivate, Album::id, emptyText) { album, selected ->
                    ZuneListRow(
                        album.title,
                        selected,
                        secondary = album.artistName,
                        leading = { AlbumArt(album.artSeed, Modifier.size(56.dp), album.art) },
                    )
                }
                LibraryTab.Songs -> TabList(library.songs, shownSelection, onActivate, Song::id, emptyText) { song, selected ->
                    ZuneListRow(song.title, selected, secondary = song.artistName)
                }
                LibraryTab.Genres -> TabList(library.genres, shownSelection, onActivate, Genre::name, emptyText) { genre, selected ->
                    ZuneListRow(genre.name.lowercase(), selected, secondary = "${genre.songCount} songs")
                }
                LibraryTab.Playlists -> TabList(library.playlists, shownSelection, onActivate, Playlist::id, emptyText) { playlist, selected ->
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
    emptyText: String,
    row: @Composable (T, Boolean) -> Unit,
) {
    if (items.isEmpty()) {
        EmptyMessage(emptyText)
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
        LibraryTab.Albums -> library.albums.getOrNull(index)?.let { album ->
            val views = AlbumView.entries
            val nextView = views[(app.settings.albumView.ordinal + 1) % views.size]
            app.showAlbumMenu(album, library, extra = listOf(MenuItem("view as: ${nextView.label}") { app.settings.albumView = nextView }))
        }
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
