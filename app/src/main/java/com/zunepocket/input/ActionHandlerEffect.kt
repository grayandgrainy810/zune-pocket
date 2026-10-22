package com.zunepocket.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf

val LocalInputRouter = staticCompositionLocalOf<InputRouter> { error("No InputRouter provided") }

/**
 * Registers [onAction] on top of the input stack for as long as the calling composable is on screen.
 * Screens and overlays that enter the composition later sit above earlier ones.
 */
@Composable
fun ActionHandlerEffect(onAction: (AppAction) -> Boolean) {
    val router = LocalInputRouter.current
    val latest by rememberUpdatedState(onAction)
    DisposableEffect(router) {
        val handler = ActionHandler { latest(it) }
        router.push(handler)
        onDispose { router.remove(handler) }
    }
}
