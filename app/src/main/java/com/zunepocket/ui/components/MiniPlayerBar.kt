package com.zunepocket.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zunepocket.ui.LocalAppState
import com.zunepocket.ui.theme.ZuneColors
import com.zunepocket.ui.theme.ZuneType

/** The strip along the bottom of browse screens showing what is playing. Tap for now playing. */
@Composable
fun MiniPlayerBar(modifier: Modifier = Modifier) {
    val app = LocalAppState.current
    val state by app.player.state.collectAsState()
    val song = state.current ?: return
    Row(
        modifier
            .fillMaxWidth()
            .clickable { app.openNowPlaying() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AlbumArt(song.artSeed, Modifier.size(40.dp), song.art)
        Column(Modifier.weight(1f)) {
            Text(song.title, style = ZuneType.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artistName.lowercase(), style = ZuneType.Caption, color = ZuneColors.Secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        TransportButton(
            if (state.isPlaying) TransportIcon.Pause else TransportIcon.Play,
            ZuneColors.Primary,
            onClick = { app.player.toggle() },
            size = 36.dp,
        )
    }
}
