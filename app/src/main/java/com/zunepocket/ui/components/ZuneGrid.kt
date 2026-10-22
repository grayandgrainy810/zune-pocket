package com.zunepocket.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zunepocket.data.Album
import com.zunepocket.ui.selection.ListSelection
import com.zunepocket.ui.theme.LocalAccent
import com.zunepocket.ui.theme.ZuneColors
import com.zunepocket.ui.theme.ZuneType
import kotlin.math.abs

private val ROW_SPACING = 14.dp

/**
 * Tile counterpart of [ZuneList]: a fixed-column grid driven by [ListSelection]. Scrolls only when
 * the selected tile's row is not fully on screen, so large tiles don't jump on every move.
 */
@Composable
fun <T> ZuneGrid(
    items: List<T>,
    selection: ListSelection,
    columns: Int,
    onActivate: (Int) -> Unit,
    modifier: Modifier = Modifier,
    key: ((T) -> Any)? = null,
    tile: @Composable (item: T, selected: Boolean) -> Unit,
) {
    val gridState = selection.gridState
    val rowSpacingPx = with(LocalDensity.current) { ROW_SPACING.roundToPx() }

    LaunchedEffect(selection.index, items.size, columns) {
        if (items.isEmpty()) return@LaunchedEffect
        val row = selection.index.coerceIn(0, items.lastIndex) / columns
        val info = gridState.layoutInfo
        val visible = info.visibleItemsInfo
        if (visible.isEmpty()) {
            gridState.scrollToItem(row * columns)
            return@LaunchedEffect
        }
        val rowHeight = visible.first().size.height + rowSpacingPx
        val viewport = info.viewportEndOffset - info.viewportStartOffset
        val rowsThatFit = (viewport / rowHeight).coerceAtLeast(1)
        val firstRow = gridState.firstVisibleItemIndex / columns
        val firstFullRow = if (gridState.firstVisibleItemScrollOffset > 0) firstRow + 1 else firstRow
        val lastFullRow = firstFullRow + rowsThatFit - 1
        val targetRow = when {
            row < firstFullRow -> row
            row > lastFullRow -> row - rowsThatFit + 1
            else -> return@LaunchedEffect
        }
        val target = targetRow.coerceAtLeast(0) * columns
        // Long jumps (page, letter) snap; single-row moves animate.
        if (abs(targetRow - firstRow) > 2) gridState.scrollToItem(target) else gridState.animateScrollToItem(target)
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = gridState,
        modifier = modifier,
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(ROW_SPACING),
    ) {
        itemsIndexed(items, key = key?.let { k -> { _: Int, item: T -> k(item) } }) { i, item ->
            Box(
                Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    selection.index = i
                    onActivate(i)
                },
            ) {
                tile(item, i == selection.index)
            }
        }
    }
}

/** Square album art with its title (and artist, on large tiles). The selected tile is outlined in the accent. */
@Composable
fun AlbumTile(album: Album, selected: Boolean, showArtist: Boolean) {
    val accent = LocalAccent.current
    val scale by animateFloatAsState(if (selected) 1.04f else 1f, label = "tileScale")
    val textColor by animateColorAsState(if (selected) ZuneColors.Primary else ZuneColors.Secondary, label = "tileText")
    Column(
        Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
    ) {
        AlbumArt(
            album.artSeed,
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .then(if (selected) Modifier.border(3.dp, accent) else Modifier),
            album.art,
        )
        Text(
            album.title,
            style = if (showArtist) ZuneType.Body else ZuneType.Caption,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 5.dp),
        )
        if (showArtist) {
            Text(
                album.artistName,
                style = ZuneType.Caption,
                color = if (selected) accent else ZuneColors.Dim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
