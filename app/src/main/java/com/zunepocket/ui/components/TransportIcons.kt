package com.zunepocket.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class TransportIcon { Play, Pause, Previous, Next }

/** Simple drawn transport glyphs, avoiding an icon-font dependency. */
@Composable
fun TransportButton(
    icon: TransportIcon,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    Canvas(
        modifier
            .size(size)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
    ) {
        val inset = this.size.minDimension * 0.25f
        when (icon) {
            TransportIcon.Play -> drawTriangle(color, inset, this.size.width - inset, pointingRight = true)
            TransportIcon.Pause -> {
                val w = (this.size.width - inset * 2) * 0.32f
                drawRect(color, Offset(inset, inset), Size(w, this.size.height - inset * 2))
                drawRect(color, Offset(this.size.width - inset - w, inset), Size(w, this.size.height - inset * 2))
            }
            TransportIcon.Previous -> {
                val bar = (this.size.width - inset * 2) * 0.14f
                drawRect(color, Offset(inset, inset), Size(bar, this.size.height - inset * 2))
                drawTriangle(color, inset + bar, this.size.width - inset, pointingRight = false)
            }
            TransportIcon.Next -> {
                val bar = (this.size.width - inset * 2) * 0.14f
                drawRect(color, Offset(this.size.width - inset - bar, inset), Size(bar, this.size.height - inset * 2))
                drawTriangle(color, inset, this.size.width - inset - bar, pointingRight = true)
            }
        }
    }
}

private fun DrawScope.drawTriangle(color: Color, left: Float, right: Float, pointingRight: Boolean) {
    val inset = size.minDimension * 0.25f
    val top = inset
    val bottom = size.height - inset
    val path = Path().apply {
        if (pointingRight) {
            moveTo(left, top); lineTo(right, (top + bottom) / 2); lineTo(left, bottom)
        } else {
            moveTo(right, top); lineTo(left, (top + bottom) / 2); lineTo(right, bottom)
        }
        close()
    }
    drawPath(path, color)
}
