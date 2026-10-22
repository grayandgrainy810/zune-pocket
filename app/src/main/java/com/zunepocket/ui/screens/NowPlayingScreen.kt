package com.zunepocket.ui.screens

import android.media.AudioManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zunepocket.input.ActionHandlerEffect
import com.zunepocket.input.AppAction
import com.zunepocket.player.Repeat
import com.zunepocket.player.formatTime
import com.zunepocket.ui.LocalAppState
import com.zunepocket.ui.components.AlbumArt
import com.zunepocket.ui.components.EmptyMessage
import com.zunepocket.ui.components.TransportButton
import com.zunepocket.ui.components.TransportIcon
import com.zunepocket.ui.components.kenBurns
import com.zunepocket.ui.theme.LocalAccent
import com.zunepocket.ui.theme.ZuneColors
import com.zunepocket.ui.theme.ZuneType

private const val SEEK_STEP_MS = 10_000L

/** Full-bleed drifting album art with oversized track text, as on the Zune HD. */
@Composable
fun NowPlayingScreen() {
    val app = LocalAppState.current
    val player = app.player
    val state by player.state.collectAsState()
    val accent = LocalAccent.current
    val context = LocalContext.current
    val audio = remember(context) { context.getSystemService(AudioManager::class.java) }

    fun volume(direction: Int) {
        audio?.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
    }

    ActionHandlerEffect { action ->
        when (action) {
            AppAction.Up -> volume(AudioManager.ADJUST_RAISE)
            AppAction.Down -> volume(AudioManager.ADJUST_LOWER)
            AppAction.Left, AppAction.PageUp -> player.previous()
            AppAction.Right, AppAction.PageDown -> player.next()
            AppAction.Confirm -> player.toggle()
            AppAction.PrevGroup -> player.seekBy(-SEEK_STEP_MS)
            AppAction.NextGroup -> player.seekBy(SEEK_STEP_MS)
            AppAction.NowPlaying -> app.openQueue()
            AppAction.Context -> app.showNowPlayingMenu()
            AppAction.JumpList -> player.toggleShuffle()
            else -> return@ActionHandlerEffect false
        }
        true
    }

    val song = state.current ?: return EmptyMessage("nothing playing")

    Box(
        Modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(Unit) {
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        val threshold = 60.dp.toPx()
                        if (total < -threshold) player.next() else if (total > threshold) player.previous()
                    },
                ) { _, dragAmount -> total += dragAmount }
            }
            .pointerInput(Unit) { detectTapGestures { player.toggle() } },
    ) {
        AnimatedContent(
            targetState = song.artSeed,
            transitionSpec = { fadeIn(tween(600)) togetherWith fadeOut(tween(600)) },
            label = "nowPlayingArt",
        ) { seed ->
            AlbumArt(seed, Modifier.fillMaxSize().kenBurns())
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.55f),
                        0.25f to Color.Transparent,
                        0.45f to Color.Black.copy(alpha = 0.25f),
                        1f to Color.Black.copy(alpha = 0.92f),
                    ),
                ),
        )

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("now playing", style = ZuneType.Caption, color = ZuneColors.Primary)
            Text("${state.index + 1} of ${state.queue.size}", style = ZuneType.Caption, color = ZuneColors.Primary)
        }

        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
            Text(
                song.artistName.lowercase(),
                style = ZuneType.NowPlayingArtist,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.Start, unbounded = true)
                    .padding(start = 18.dp),
            )
            Text(
                song.albumTitle.lowercase(),
                style = ZuneType.Body,
                color = ZuneColors.Secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Text(
                song.title,
                style = ZuneType.NowPlayingTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 2.dp),
            )
            Spacer(Modifier.height(10.dp))
            ProgressBar(state.positionMs.toFloat() / song.durationMs, accent, Modifier.padding(horizontal = 20.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(state.positionMs), style = ZuneType.Caption, color = ZuneColors.Secondary)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (state.shuffle) Text("shuffle", style = ZuneType.Caption, color = accent)
                    when (state.repeat) {
                        Repeat.All -> Text("repeat", style = ZuneType.Caption, color = accent)
                        Repeat.One -> Text("repeat one", style = ZuneType.Caption, color = accent)
                        Repeat.Off -> Unit
                    }
                }
                Text("-" + formatTime(song.durationMs - state.positionMs), style = ZuneType.Caption, color = ZuneColors.Secondary)
            }
            Row(
                Modifier.fillMaxWidth().padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
            ) {
                TransportButton(TransportIcon.Previous, ZuneColors.Primary, onClick = { player.previous() })
                TransportButton(
                    if (state.isPlaying) TransportIcon.Pause else TransportIcon.Play,
                    ZuneColors.Primary,
                    onClick = { player.toggle() },
                    size = 52.dp,
                )
                TransportButton(TransportIcon.Next, ZuneColors.Primary, onClick = { player.next() })
            }
        }
    }
}

@Composable
private fun ProgressBar(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(3.dp)
            .drawBehind {
                drawRect(ZuneColors.Dim)
                drawRect(color, Offset.Zero, Size(size.width * fraction.coerceIn(0f, 1f), size.height))
            },
    )
}
