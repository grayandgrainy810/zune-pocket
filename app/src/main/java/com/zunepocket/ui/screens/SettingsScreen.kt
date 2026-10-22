package com.zunepocket.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zunepocket.input.ActionHandlerEffect
import com.zunepocket.input.AppAction
import com.zunepocket.nav.Screen
import com.zunepocket.ui.LocalAppState
import com.zunepocket.ui.components.PanoramaTitle
import com.zunepocket.ui.components.ZuneList
import com.zunepocket.ui.components.ZuneListRow
import com.zunepocket.ui.theme.Accent
import com.zunepocket.ui.theme.ZuneType

private class SettingRow(val label: String, val value: String, val change: (step: Int) -> Unit)

/** A/Right/Left change the selected setting. */
@Composable
fun SettingsScreen(screen: Screen.Settings) {
    val app = LocalAppState.current
    val settings = app.settings

    val rows = listOf(
        SettingRow("music source", if (settings.useSampleLibrary) "sample music (silent demo)" else "your music") {
            settings.useSampleLibrary = !settings.useSampleLibrary
        },
        SettingRow("accent color", settings.accent.label) { step ->
            val all = Accent.entries
            settings.accent = all[(settings.accent.ordinal + step + all.size) % all.size]
        },
        SettingRow("confirm button", if (settings.swapConfirmBack) "B confirms, A goes back" else "A confirms, B goes back") {
            settings.swapConfirmBack = !settings.swapConfirmBack
        },
        SettingRow("keep screen on", if (settings.keepScreenOn) "on" else "off") {
            settings.keepScreenOn = !settings.keepScreenOn
        },
        SettingRow("key tester", "see what each button sends") { app.open(Screen.KeyTester()) },
        SettingRow("about", "zune pocket 0.2") {},
    )

    ActionHandlerEffect { action ->
        val row = rows.getOrNull(screen.selection.index)
        when (action) {
            AppAction.Confirm, AppAction.Right -> row?.change?.invoke(1)
            AppAction.Left -> row?.change?.invoke(-1)
            else -> return@ActionHandlerEffect screen.selection.handle(action, rows.size)
        }
        true
    }

    Column(Modifier.fillMaxSize()) {
        PanoramaTitle("settings", null, style = ZuneType.Header)
        ZuneList(rows, screen.selection, { rows[it].change(1) }, Modifier.fillMaxSize()) { row, selected ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (row.label == "accent color") {
                    Box(Modifier.size(28.dp).background(settings.accent.color))
                    Spacer(Modifier.width(12.dp))
                }
                ZuneListRow(row.label, selected, secondary = row.value)
            }
        }
    }
}
