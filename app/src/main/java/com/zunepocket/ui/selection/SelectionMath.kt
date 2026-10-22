package com.zunepocket.ui.selection

/** Pure index arithmetic for list navigation, kept free of Compose so it can be unit-tested. */
object SelectionMath {
    const val PAGE_SIZE = 7

    fun move(index: Int, delta: Int, size: Int): Int =
        if (size <= 0) 0 else (index + delta).coerceIn(0, size - 1)

    fun groupStart(index: Int, letterAt: (Int) -> Char): Int {
        val letter = letterAt(index)
        var i = index
        while (i > 0 && letterAt(i - 1) == letter) i--
        return i
    }

    /** First item of the next letter group, or the last item if already in the final group. */
    fun nextGroup(index: Int, size: Int, letterAt: (Int) -> Char): Int {
        if (size <= 0) return 0
        val letter = letterAt(index)
        var i = index + 1
        while (i < size && letterAt(i) == letter) i++
        return if (i < size) i else size - 1
    }

    /** Start of the current group, or of the previous group if already at the start. */
    fun prevGroup(index: Int, size: Int, letterAt: (Int) -> Char): Int {
        if (size <= 0) return 0
        val start = groupStart(index, letterAt)
        if (start < index || start == 0) return start
        return groupStart(start - 1, letterAt)
    }

    /** First item filed under [letter] or the closest letter after it ('#' sorts first). */
    fun indexForLetter(letter: Char, size: Int, letterAt: (Int) -> Char): Int {
        if (size <= 0) return 0
        val target = rank(letter)
        for (i in 0 until size) if (rank(letterAt(i)) >= target) return i
        return size - 1
    }

    const val GRID_PAGE_ROWS = 3

    fun gridUp(index: Int, columns: Int): Int = if (index - columns >= 0) index - columns else index

    /** One row down; from the row above a short last row, lands on the last item. */
    fun gridDown(index: Int, size: Int, columns: Int): Int {
        if (size <= 0) return 0
        val target = index + columns
        return when {
            target < size -> target
            (size - 1) / columns > index / columns -> size - 1
            else -> index
        }
    }

    /** One tile left, or null at the start of a row (the screen switches pivot instead). */
    fun gridLeft(index: Int, columns: Int): Int? = if (index % columns == 0) null else index - 1

    /** One tile right, or null at the end of a row or of the list. */
    fun gridRight(index: Int, size: Int, columns: Int): Int? =
        if (index % columns == columns - 1 || index >= size - 1) null else index + 1

    private fun rank(c: Char): Int = if (c == '#') 0 else c - 'A' + 1
}
