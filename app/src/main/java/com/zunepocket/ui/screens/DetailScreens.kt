package com.zunepocket.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zunepocket.data.Album
import com.zunepocket.data.MusicLibrary
import com.zunepocket.data.Song
import com.zunepocket.input.ActionHandlerEffect
import com.zunepocket.input.AppAction
import com.zunepocket.nav.Screen
import com.zunepocket.player.formatTime
import com.zunepocket.ui.LocalAppState
import com.zunepocket.ui.MenuItem
import com.zunepocket.ui.components.AlbumArt
import com.zunepocket.ui.components.EmptyMessage
import com.zunepocket.ui.components.PanoramaTitle
import com.zunepocket.ui.components.PivotHeader
import com.zunepocket.ui.components.ZuneList
import com.zunepocket.ui.components.ZuneListRow
import com.zunepocket.ui.components.pivotSwipe
import com.zunepocket.ui.theme.LocalAccent
import com.zunepocket.ui.theme.ZuneColors
import com.zunepocket.ui.theme.ZuneType

private val ARTIST_PIVOTS = listOf("albums", "songs")

/** An artist: albums / songs pivots under the artist's name. */
@Composable
fun ArtistScreen(screen: Screen.ArtistDetail, library: MusicLibrary) {
    val app = LocalAppState.current
    val artist = library.artist(screen.artistId) ?: return EmptyMessage("artist not found")
    val albums = library.albumsBy(artist.id)
    val songs = library.songsBy(artist.id)
    val showingAlbums = screen.pivot.index == 0
    val selection = screen.selections[screen.pivot.index]

    fun activate(onAlbums: Boolean, i: Int) {
        if (onAlbums) albums.getOrNull(i)?.let { app.open(Screen.AlbumDetail(it.id)) }
        else if (i in songs.indices) app.play(songs, i)
    }

    ActionHandlerEffect { action ->
        when (action) {
            AppAction.Left -> screen.pivot.previous()
            AppAction.Right -> screen.pivot.next()
            AppAction.Confirm -> activate(showingAlbums, selection.index)
            AppAction.Context ->
                if (showingAlbums) albums.getOrNull(selection.index)?.let { app.showAlbumMenu(it, library) }
                else songs.getOrNull(selection.index)?.let { app.showSongMenu(it, library) }
            else -> return@ActionHandlerEffect selection.handle(action, if (showingAlbums) albums.size else songs.size)
        }
        true
    }

    Column(Modifier.fillMaxSize().pivotSwipe(screen.pivot)) {
        PanoramaTitle(artist.name, screen.pivot, style = ZuneType.Header)
        PivotHeader(ARTIST_PIVOTS, screen.pivot)
        AnimatedContent(
            targetState = screen.pivot.index,
            transitionSpec = {
                (slideInHorizontally(tween(260)) { it / 5 * screen.pivot.direction } + fadeIn(tween(220))) togetherWith fadeOut(tween(120))
            },
            label = "artistPivot",
        ) { page ->
            if (page == 0) {
                ZuneList(albums, screen.selections[0], { activate(true, it) }, Modifier.fillMaxSize(), key = Album::id) { album, selected ->
                    ZuneListRow(
                        album.title,
                        selected,
                        secondary = if (album.year > 0) "${album.year}" else null,
                        leading = { AlbumArt(album.artSeed, Modifier.size(72.dp), album.art) },
                    )
                }
            } else {
                ZuneList(songs, screen.selections[1], { activate(false, it) }, Modifier.fillMaxSize(), key = Song::id) { song, selected ->
                    ZuneListRow(song.title, selected, secondary = song.albumTitle, trailing = formatTime(song.durationMs))
                }
            }
        }
    }
}

/** An album: large art and details above the track list. */
@Composable
fun AlbumScreen(screen: Screen.AlbumDetail, library: MusicLibrary) {
    val app = LocalAppState.current
    val album = library.album(screen.albumId) ?: return EmptyMessage("album not found")
    val songs = library.songsOn(album.id)
    val accent = LocalAccent.current

    ActionHandlerEffect { action ->
        when (action) {
            AppAction.Confirm -> app.play(songs, screen.selection.index)
            AppAction.Context -> songs.getOrNull(screen.selection.index)?.let { app.showSongMenu(it, library) }
            else -> return@ActionHandlerEffect screen.selection.handle(action, songs.size)
        }
        true
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            AlbumArt(album.artSeed, Modifier.size(120.dp), album.art)
            Column {
                Text(album.title.lowercase(), style = ZuneType.ListPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(album.artistName.lowercase(), style = ZuneType.Body, color = accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(listOfNotNull(album.year.takeIf { it > 0 }, "${songs.size} songs").joinToString(" · "), style = ZuneType.Caption, color = ZuneColors.Secondary)
            }
        }
        ZuneList(songs, screen.selection, { app.play(songs, it) }, Modifier.fillMaxSize(), key = Song::id) { song, selected ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${song.track}",
                    style = ZuneType.Body,
                    color = if (selected) accent else ZuneColors.Dim,
                    modifier = Modifier.width(30.dp),
                )
                ZuneListRow(song.title, selected, trailing = formatTime(song.durationMs))
            }
        }
    }
}

/** A genre or playlist. */
@Composable
fun SongListScreen(screen: Screen.SongList, library: MusicLibrary) {
    val app = LocalAppState.current
    val songs = screen.songs(library)

    ActionHandlerEffect { action ->
        when (action) {
            AppAction.Confirm -> app.play(songs, screen.selection.index)
            AppAction.Context -> songs.getOrNull(screen.selection.index)?.let { song ->
                app.showMenu(
                    song.title,
                    listOf(
                        MenuItem("play from here") { app.play(songs, screen.selection.index) },
                        MenuItem("shuffle all") { app.shuffleAll(songs) },
                        MenuItem("play next") { app.player.playNext(listOf(song)) },
                        MenuItem("add to now playing") { app.player.addToQueue(listOf(song)) },
                        MenuItem("go to album") { app.open(Screen.AlbumDetail(song.albumId)) },
                        MenuItem("go to artist") { app.open(Screen.ArtistDetail(song.artistId)) },
                    ),
                )
            }
            else -> return@ActionHandlerEffect screen.selection.handle(action, songs.size)
        }
        true
    }

    Column(Modifier.fillMaxSize()) {
        PanoramaTitle(screen.title, null, style = ZuneType.Header)
        Text(
            "${songs.size} songs",
            style = ZuneType.Caption,
            color = ZuneColors.Secondary,
            modifier = Modifier.padding(start = 20.dp, bottom = 6.dp),
        )
        if (songs.isEmpty()) {
            EmptyMessage("no songs")
        } else {
            ZuneList(songs, screen.selection, { app.play(songs, it) }, Modifier.fillMaxSize()) { song, selected ->
                ZuneListRow(song.title, selected, secondary = song.artistName, trailing = formatTime(song.durationMs))
            }
        }
    }
}
