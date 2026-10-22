package com.zunepocket.ui.selection

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.zunepocket.input.AppAction

/**
 * The selected row of a list, owned by the screen rather than by Compose's focus system so that
 * buttons and touch always agree. Lives in the screen's back-stack entry, so it survives navigation.
 */
@Stable
class ListSelection(initialIndex: Int = 0) {
    var index by mutableIntStateOf(initialIndex)
    val listState = LazyListState(firstVisibleItemIndex = (initialIndex - 2).coerceAtLeast(0))
    val gridState = LazyGridState(firstVisibleItemIndex = initialIndex)

    /** Handles the list-movement actions. [letterAt] enables L2/R2 letter-group jumps. */
    fun handle(action: AppAction, size: Int, letterAt: ((Int) -> Char)? = null): Boolean {
        if (size == 0) return false
        index = index.coerceIn(0, size - 1)
        index = when (action) {
            AppAction.Up -> SelectionMath.move(index, -1, size)
            AppAction.Down -> SelectionMath.move(index, 1, size)
            AppAction.PageUp -> SelectionMath.move(index, -SelectionMath.PAGE_SIZE, size)
            AppAction.PageDown -> SelectionMath.move(index, SelectionMath.PAGE_SIZE, size)
            AppAction.PrevGroup -> letterAt?.let { SelectionMath.prevGroup(index, size, it) } ?: return false
            AppAction.NextGroup -> letterAt?.let { SelectionMath.nextGroup(index, size, it) } ?: return false
            else -> return false
        }
        return true
    }

    /**
     * Grid version of [handle] for a [columns]-wide tile layout. Returns null when Left/Right hits
     * the edge of a row (the caller switches pivot), true when handled, false when not a grid action.
     */
    fun handleGrid(action: AppAction, size: Int, columns: Int, letterAt: ((Int) -> Char)? = null): Boolean? {
        if (size == 0) return if (action == AppAction.Left || action == AppAction.Right) null else false
        index = index.coerceIn(0, size - 1)
        val page = SelectionMath.GRID_PAGE_ROWS * columns
        index = when (action) {
            AppAction.Up -> SelectionMath.gridUp(index, columns)
            AppAction.Down -> SelectionMath.gridDown(index, size, columns)
            AppAction.Left -> SelectionMath.gridLeft(index, columns) ?: return null
            AppAction.Right -> SelectionMath.gridRight(index, size, columns) ?: return null
            AppAction.PageUp -> SelectionMath.move(index, -page, size)
            AppAction.PageDown -> SelectionMath.move(index, page, size)
            AppAction.PrevGroup -> letterAt?.let { SelectionMath.prevGroup(index, size, it) } ?: return false
            AppAction.NextGroup -> letterAt?.let { SelectionMath.nextGroup(index, size, it) } ?: return false
            else -> return false
        }
        return true
    }
}

/** Which pivot (tab) is showing, plus the direction of the last change for slide animations. */
@Stable
class PivotState(val count: Int, initialIndex: Int = 0) {
    var index by mutableIntStateOf(initialIndex)
        private set
    var direction by mutableIntStateOf(1)
        private set

    fun next() {
        direction = 1
        index = (index + 1) % count
    }

    fun previous() {
        direction = -1
        index = (index - 1 + count) % count
    }

    fun select(target: Int) {
        if (target == index) return
        direction = if (target > index) 1 else -1
        index = target
    }
}
