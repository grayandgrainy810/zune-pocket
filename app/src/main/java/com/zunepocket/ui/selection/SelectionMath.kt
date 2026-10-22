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

    private fun rank(c: Char): Int = if (c == '#') 0 else c - 'A' + 1
}
