package com.zunepocket.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zunepocket.data.JUMP_LETTERS
import com.zunepocket.input.ActionHandlerEffect
import com.zunepocket.input.AppAction
import com.zunepocket.ui.LocalAppState
import com.zunepocket.ui.Overlay
import com.zunepocket.ui.theme.LocalAccent
import com.zunepocket.ui.theme.ZuneColors
import com.zunepocket.ui.theme.ZuneType

@Composable
fun OverlayHost(overlay: Overlay) {
    when (overlay) {
        is Overlay.Menu -> ContextMenu(overlay)
        is Overlay.JumpList -> JumpList(overlay)
    }
}

/** Full-screen context menu (Y button). Modal: consumes everything except play/pause. */
@Composable
private fun ContextMenu(menu: Overlay.Menu) {
    val app = LocalAppState.current
    fun activate(i: Int) {
        app.dismissOverlay()
        menu.items.getOrNull(i)?.action?.invoke()
    }
    ActionHandlerEffect { action ->
        when (action) {
            AppAction.Confirm -> activate(menu.selection.index)
            AppAction.Back, AppAction.Context -> app.dismissOverlay()
            AppAction.PlayPause -> return@ActionHandlerEffect false
            else -> menu.selection.handle(action, menu.items.size)
        }
        true
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(ZuneColors.Scrim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { app.dismissOverlay() },
    ) {
        Text(
            menu.title.lowercase(),
            style = ZuneType.Body,
            color = ZuneColors.Secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 8.dp),
        )
        ZuneList(menu.items, menu.selection, onActivate = ::activate) { item, selected ->
            ZuneListRow(item.label, selected, primaryStyle = ZuneType.MenuItem)
        }
    }
}

/** The Zune's letter grid (Select button): jump straight to a letter in a long list. */
@Composable
private fun JumpList(jump: Overlay.JumpList) {
    val app = LocalAppState.current
    val accent = LocalAccent.current
    var index by remember { mutableIntStateOf(JUMP_LETTERS.indexOf(jump.initial).coerceAtLeast(0)) }
    fun pick(i: Int) {
        app.dismissOverlay()
        jump.onPick(JUMP_LETTERS[i])
    }
    ActionHandlerEffect { action ->
        val last = JUMP_LETTERS.lastIndex
        when (action) {
            AppAction.Left -> index = (index - 1).coerceAtLeast(0)
            AppAction.Right -> index = (index + 1).coerceAtMost(last)
            AppAction.Up -> if (index - COLUMNS >= 0) index -= COLUMNS
            AppAction.Down -> index = (index + COLUMNS).coerceAtMost(last)
            AppAction.Confirm -> pick(index)
            AppAction.Back, AppAction.JumpList -> app.dismissOverlay()
            AppAction.PlayPause -> return@ActionHandlerEffect false
            else -> Unit
        }
        true
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(ZuneColors.Scrim)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        JUMP_LETTERS.chunked(COLUMNS).forEachIndexed { row, letters ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                letters.forEachIndexed { col, letter ->
                    val i = row * COLUMNS + col
                    val available = letter in jump.available
                    val selected = i == index
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(
                                when {
                                    selected -> ZuneColors.Primary
                                    available -> accent
                                    else -> ZuneColors.Dim.copy(alpha = 0.5f)
                                },
                            )
                            .clickable { pick(i) },
                        contentAlignment = Alignment.BottomStart,
                    ) {
                        Text(
                            letter.lowercaseChar().toString(),
                            style = ZuneType.JumpLetter,
                            color = when {
                                selected -> accent
                                available -> ZuneColors.Primary
                                else -> ZuneColors.Secondary
                            },
                            modifier = Modifier.padding(start = 6.dp, bottom = 2.dp),
                        )
                    }
                }
                // Keep the last row's tiles the same size as the others.
                repeat(COLUMNS - letters.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

private const val COLUMNS = 6
