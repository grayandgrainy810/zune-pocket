package com.zunepocket.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zunepocket.ui.selection.ListSelection
import com.zunepocket.ui.theme.LocalAccent
import com.zunepocket.ui.theme.ZuneColors
import com.zunepocket.ui.theme.ZuneType
import kotlin.math.abs

/**
 * A list driven by [ListSelection] instead of Compose focus. Keeps the selected row inside a
 * comfortable window while the D-pad moves it; tapping a row selects and activates it.
 */
@Composable
fun <T> ZuneList(
    items: List<T>,
    selection: ListSelection,
    onActivate: (Int) -> Unit,
    modifier: Modifier = Modifier,
    key: ((T) -> Any)? = null,
    contentPadding: PaddingValues = PaddingValues(top = 4.dp, bottom = 24.dp),
    itemContent: @Composable (item: T, selected: Boolean) -> Unit,
) {
    val listState = selection.listState
    LaunchedEffect(selection.index, items.size) {
        if (items.isEmpty()) return@LaunchedEffect
        val index = selection.index.coerceIn(0, items.lastIndex)
        val visible = listState.layoutInfo.visibleItemsInfo
        if (visible.isEmpty()) {
            listState.scrollToItem((index - MARGIN).coerceAtLeast(0))
            return@LaunchedEffect
        }
        val first = visible.first().index
        val last = visible.last().index
        val target = when {
            index < first + MARGIN -> (index - MARGIN).coerceAtLeast(0)
            index > last - MARGIN -> (index - (last - first) + MARGIN).coerceAtLeast(0)
            else -> return@LaunchedEffect
        }
        // Long jumps (page, letter) snap; short moves animate.
        if (abs(target - first) > 8) listState.scrollToItem(target) else listState.animateScrollToItem(target)
    }

    LazyColumn(state = listState, modifier = modifier, contentPadding = contentPadding) {
        itemsIndexed(items, key = key?.let { k -> { _: Int, item: T -> k(item) } }) { i, item ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        selection.index = i
                        onActivate(i)
                    }
                    .padding(horizontal = 20.dp, vertical = 5.dp),
            ) {
                itemContent(item, i == selection.index)
            }
        }
    }
}

private const val MARGIN = 2

/** Standard row: the selected row is bright and slightly enlarged, the rest recede. */
@Composable
fun ZuneListRow(
    primary: String,
    selected: Boolean,
    secondary: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: String? = null,
    primaryStyle: TextStyle = ZuneType.ListPrimary,
    primaryColor: Color? = null,
) {
    val scale by animateFloatAsState(if (selected) 1.05f else 1f, label = "rowScale")
    val textColor by animateColorAsState(primaryColor ?: if (selected) ZuneColors.Primary else ZuneColors.Secondary, label = "rowColor")
    val secondaryColor = if (selected) LocalAccent.current else ZuneColors.Dim
    Row(
        Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin(0f, 0.5f)
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f)) {
            Text(primary, style = primaryStyle, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (secondary != null) {
                Text(secondary, style = ZuneType.ListSecondary, color = secondaryColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (trailing != null) {
            Text(trailing, style = ZuneType.ListSecondary, color = if (selected) ZuneColors.Primary else ZuneColors.Dim)
        }
    }
}

/** Shown in place of a list with nothing in it. */
@Composable
fun EmptyMessage(text: String, modifier: Modifier = Modifier) {
    Text(text, style = ZuneType.ListPrimary, color = ZuneColors.Secondary, modifier = modifier.padding(20.dp))
}
