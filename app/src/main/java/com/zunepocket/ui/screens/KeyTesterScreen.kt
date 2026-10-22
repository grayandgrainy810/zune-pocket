package com.zunepocket.ui.screens

import android.view.KeyEvent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zunepocket.input.LocalInputRouter
import com.zunepocket.ui.LocalAppState
import com.zunepocket.ui.theme.LocalAccent
import com.zunepocket.ui.theme.ZuneColors
import com.zunepocket.ui.theme.ZuneType

private class KeyLog(val name: String, val detail: String, val mapped: String)

/**
 * Shows exactly what each physical button sends, so the bindings can be checked on real hardware.
 * Every key is captured here (except volume), so exit by holding any button for 1.5 s, or by tapping.
 */
@Composable
fun KeyTesterScreen() {
    val app = LocalAppState.current
    val router = LocalInputRouter.current
    val bindings = app.container.keyBindings
    val log = remember { mutableStateListOf<KeyLog>() }

    DisposableEffect(router) {
        router.rawListener = listener@{ event ->
            if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP || event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                return@listener false
            }
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount > 0) return@listener true
            val down = event.action == KeyEvent.ACTION_DOWN
            val name = KeyEvent.keyCodeToString(event.keyCode).removePrefix("KEYCODE_")
            log.add(
                0,
                KeyLog(
                    name = name,
                    detail = "${if (down) "down" else "up"} · code ${event.keyCode} · scan ${event.scanCode} · ${event.device?.name ?: "unknown device"}",
                    mapped = bindings.map(event.keyCode)?.name?.lowercase() ?: "not mapped",
                ),
            )
            if (log.size > 40) log.removeAt(log.lastIndex)
            if (!down && event.eventTime - event.downTime >= EXIT_HOLD_MS) app.navigator.pop()
            true
        }
        onDispose { router.rawListener = null }
    }

    val accent = LocalAccent.current
    Column(Modifier.fillMaxSize().clickable { app.navigator.pop() }) {
        Text("key tester", style = ZuneType.Header, modifier = Modifier.padding(start = 18.dp, top = 12.dp))
        Text(
            "press any button · hold one for 1.5 s or tap to exit",
            style = ZuneType.Caption,
            color = ZuneColors.Secondary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            items(log) { entry ->
                Column(Modifier.padding(vertical = 4.dp)) {
                    Text("${entry.name}  →  ${entry.mapped}", style = ZuneType.Body, color = if (entry === log.firstOrNull()) accent else ZuneColors.Primary)
                    Text(entry.detail, style = ZuneType.Caption, color = ZuneColors.Secondary)
                }
            }
        }
    }
}

private const val EXIT_HOLD_MS = 1_500L
