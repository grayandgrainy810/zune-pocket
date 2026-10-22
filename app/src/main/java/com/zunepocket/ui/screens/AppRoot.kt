package com.zunepocket.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.zunepocket.input.ActionHandlerEffect
import com.zunepocket.input.AppAction
import com.zunepocket.input.LocalInputRouter
import com.zunepocket.nav.Screen
import com.zunepocket.ui.AppState
import com.zunepocket.ui.LocalAppState
import com.zunepocket.ui.components.LocalArtLoader
import com.zunepocket.ui.components.MiniPlayerBar
import com.zunepocket.ui.components.OverlayHost
import com.zunepocket.ui.theme.ZuneColors
import com.zunepocket.ui.theme.ZuneTheme

@Composable
fun AppRoot(app: AppState, onExit: () -> Unit) {
    val library by app.container.repository.library.collectAsState()
    ZuneTheme(accent = app.settings.accent.color) {
        CompositionLocalProvider(
            LocalInputRouter provides app.container.inputRouter,
            LocalAppState provides app,
            LocalArtLoader provides app.container.artLoader,
        ) {
            // Global bindings: the bottom of the handler stack, reached when no screen consumes an action.
            ActionHandlerEffect { action ->
                when (action) {
                    AppAction.PlayPause -> app.player.toggle()
                    AppAction.NowPlaying -> app.openNowPlaying()
                    AppAction.NextTrack -> app.player.next()
                    AppAction.PreviousTrack -> app.player.previous()
                    AppAction.Back -> if (!app.navigator.pop()) onExit()
                    else -> return@ActionHandlerEffect false
                }
                true
            }
            Box(Modifier.fillMaxSize().background(ZuneColors.Background)) {
                AnimatedContent(
                    targetState = app.navigator.current,
                    transitionSpec = {
                        val dir = if (app.navigator.lastWasPop) -1 else 1
                        (slideInHorizontally(tween(260)) { it / 4 * dir } + fadeIn(tween(220))) togetherWith
                            (slideOutHorizontally(tween(220)) { -it / 4 * dir } + fadeOut(tween(160)))
                    },
                    label = "screens",
                ) { screen ->
                    when (screen) {
                        is Screen.Home -> HomeScreen(screen, library)
                        is Screen.Library -> WithMiniPlayer { LibraryScreen(screen, library) }
                        is Screen.ArtistDetail -> WithMiniPlayer { ArtistScreen(screen, library) }
                        is Screen.AlbumDetail -> WithMiniPlayer { AlbumScreen(screen, library) }
                        is Screen.SongList -> WithMiniPlayer { SongListScreen(screen, library) }
                        is Screen.NowPlaying -> NowPlayingScreen()
                        is Screen.Queue -> QueueScreen(screen)
                        is Screen.Settings -> SettingsScreen(screen)
                        is Screen.KeyTester -> KeyTesterScreen()
                    }
                }
                app.overlay?.let { OverlayHost(it) }
            }
        }
    }
}

@Composable
private fun WithMiniPlayer(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
        MiniPlayerBar()
    }
}
