package com.zunepocket.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.zunepocket.input.ActionHandlerEffect
import com.zunepocket.input.AppAction
import com.zunepocket.nav.Screen
import com.zunepocket.player.formatTime
import com.zunepocket.ui.LocalAppState
import com.zunepocket.ui.MenuItem
import com.zunepocket.ui.components.EmptyMessage
import com.zunepocket.ui.components.PanoramaTitle
import com.zunepocket.ui.components.ZuneList
import com.zunepocket.ui.components.ZuneListRow
import com.zunepocket.ui.theme.LocalAccent
import com.zunepocket.ui.theme.ZuneType

/** The up-next list. The playing song is shown in the accent colour. */
@Composable
fun QueueScreen(screen: Screen.Queue) {
    val app = LocalAppState.current
    val state by app.player.state.collectAsState()
    val queue = state.queue
    val accent = LocalAccent.current

    fun jump(i: Int) {
        app.player.jumpTo(i)
        app.navigator.pop()
    }

    ActionHandlerEffect { action ->
        when (action) {
            AppAction.Confirm -> jump(screen.selection.index)
            AppAction.Context -> queue.getOrNull(screen.selection.index)?.let { song ->
                val i = screen.selection.index
                app.showMenu(
                    song.title,
                    listOfNotNull(
                        MenuItem("play") { jump(i) },
                        if (i != state.index) MenuItem("remove from now playing") { app.player.removeFromQueue(i) } else null,
                        MenuItem("go to album") { app.open(Screen.AlbumDetail(song.albumId)) },
                    ),
                )
            }
            else -> return@ActionHandlerEffect screen.selection.handle(action, queue.size)
        }
        true
    }

    Column(Modifier.fillMaxSize()) {
        PanoramaTitle("now playing", null)
        PanoramaTitle("queue", null, style = ZuneType.Header)
        if (queue.isEmpty()) {
            EmptyMessage("the queue is empty")
        } else {
            ZuneList(queue.withIndex().toList(), screen.selection, ::jump, Modifier.fillMaxSize()) { (i, song), selected ->
                ZuneListRow(
                    song.title,
                    selected,
                    secondary = song.artistName,
                    trailing = formatTime(song.durationMs),
                    primaryColor = if (i == state.index) accent else null,
                )
            }
        }
    }
}
