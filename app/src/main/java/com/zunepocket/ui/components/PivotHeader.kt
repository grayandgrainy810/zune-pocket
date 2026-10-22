package com.zunepocket.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.zunepocket.ui.selection.PivotState
import com.zunepocket.ui.theme.ZuneColors
import com.zunepocket.ui.theme.ZuneType

/**
 * The small grey section title above a pivot ("music"), drifting left as pivots advance: a hint of
 * the Zune HD's panorama parallax.
 */
@Composable
fun PanoramaTitle(text: String, pivot: PivotState?, modifier: Modifier = Modifier, style: TextStyle = ZuneType.Panorama) {
    val offset by animateDpAsState((-(pivot?.index ?: 0) * 10).dp, tween(350), label = "panorama")
    Text(
        text.lowercase(),
        style = style,
        color = ZuneColors.Secondary,
        maxLines = 1,
        softWrap = false,
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .wrapContentWidth(Alignment.Start, unbounded = true)
            .padding(start = 18.dp, top = 10.dp)
            .offset(x = offset),
    )
}

/**
 * Oversized lowercase headings: the current one in white, the rest trailing off the right edge in
 * grey, wrapping around like the Zune's pivots.
 */
@Composable
fun PivotHeader(titles: List<String>, pivot: PivotState, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = pivot.index,
        transitionSpec = {
            val dir = pivot.direction
            (slideInHorizontally(tween(260)) { it / 3 * dir } + fadeIn(tween(220))) togetherWith
                (slideOutHorizontally(tween(200)) { -it / 3 * dir } + fadeOut(tween(150)))
        },
        modifier = modifier.fillMaxWidth().clipToBounds(),
        label = "pivotHeader",
    ) { selected ->
        Row(
            Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.Start, unbounded = true)
                .padding(start = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            for (k in titles.indices) {
                val i = (selected + k) % titles.size
                Text(
                    titles[i],
                    style = ZuneType.Pivot,
                    color = if (k == 0) ZuneColors.Primary else ZuneColors.Dim,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { pivot.select(i) },
                )
            }
        }
    }
}

/** Swipe left/right to change pivot, mirroring the D-pad. */
fun Modifier.pivotSwipe(pivot: PivotState): Modifier = pointerInput(pivot) {
    var total = 0f
    detectHorizontalDragGestures(
        onDragStart = { total = 0f },
        onDragEnd = {
            val threshold = 60.dp.toPx()
            if (total < -threshold) pivot.next() else if (total > threshold) pivot.previous()
        },
    ) { _, dragAmount -> total += dragAmount }
}
