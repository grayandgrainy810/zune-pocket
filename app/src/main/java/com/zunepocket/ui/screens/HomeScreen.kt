package com.zunepocket.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.zunepocket.data.MusicLibrary
import com.zunepocket.input.ActionHandlerEffect
import com.zunepocket.input.AppAction
import com.zunepocket.nav.LibraryTab
import com.zunepocket.nav.Screen
import com.zunepocket.ui.LocalAppState
import com.zunepocket.ui.components.AlbumArt
import com.zunepocket.ui.components.MiniPlayerBar
import com.zunepocket.ui.components.ZuneList
import com.zunepocket.ui.components.ZuneListRow
import com.zunepocket.ui.components.kenBurns
import com.zunepocket.ui.theme.ZuneColors
import com.zunepocket.ui.theme.ZuneType

private class HomeItem(val label: String, val enabled: Boolean = true, val action: () -> Unit)

/** The Zune HD-style main menu: huge words, with the playing album faintly behind them. */
@Composable
fun HomeScreen(screen: Screen.Home, library: MusicLibrary) {
    val app = LocalAppState.current
    val playback by app.player.state.collectAsState()
    val current = playback.current

    val items = listOf(
        HomeItem("music") { app.open(Screen.Library(LibraryTab.Artists)) },
        HomeItem("playlists") { app.open(Screen.Library(LibraryTab.Playlists)) },
        HomeItem("now playing", enabled = current != null) { app.openNowPlaying() },
        HomeItem("shuffle all", enabled = library.songs.isNotEmpty()) { app.shuffleAll(library.songs) },
        HomeItem("settings") { app.open(Screen.Settings()) },
    )
    fun activate(i: Int) {
        items.getOrNull(i)?.takeIf { it.enabled }?.action?.invoke()
    }

    ActionHandlerEffect { action ->
        when (action) {
            AppAction.Confirm, AppAction.Right -> {
                activate(screen.selection.index)
                true
            }
            else -> screen.selection.handle(action, items.size)
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (current != null) {
            AlbumArt(current.artSeed, Modifier.fillMaxSize().kenBurns().alpha(0.22f))
        }
        Column(Modifier.fillMaxSize()) {
            Text(
                "zune pocket",
                style = ZuneType.Panorama,
                color = ZuneColors.Secondary,
                modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 4.dp),
            )
            ZuneList(items, screen.selection, onActivate = ::activate, modifier = Modifier.weight(1f).fillMaxWidth()) { item, selected ->
                ZuneListRow(
                    item.label,
                    selected,
                    primaryStyle = ZuneType.HomeItem,
                    primaryColor = when {
                        !item.enabled -> ZuneColors.Dim
                        selected -> ZuneColors.Primary
                        else -> ZuneColors.Secondary
                    },
                )
            }
            MiniPlayerBar()
        }
    }
}
