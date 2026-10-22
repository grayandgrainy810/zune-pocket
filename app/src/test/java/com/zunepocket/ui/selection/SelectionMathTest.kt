package com.zunepocket.ui.selection

import org.junit.Assert.assertEquals
import org.junit.Test

class SelectionMathTest {
    // Index:          0    1    2    3    4    5    6
    private val letters = listOf('#', 'A', 'A', 'B', 'D', 'D', 'D')
    private val letterAt = { i: Int -> letters[i] }
    private val size = letters.size

    @Test
    fun `move clamps to the list`() {
        assertEquals(0, SelectionMath.move(0, -1, size))
        assertEquals(6, SelectionMath.move(5, 7, size))
        assertEquals(3, SelectionMath.move(1, 2, size))
        assertEquals(0, SelectionMath.move(3, 1, 0))
    }

    @Test
    fun `next group jumps to the first item of the following letter`() {
        assertEquals(1, SelectionMath.nextGroup(0, size, letterAt))
        assertEquals(3, SelectionMath.nextGroup(1, size, letterAt))
        assertEquals(3, SelectionMath.nextGroup(2, size, letterAt))
        assertEquals(6, SelectionMath.nextGroup(4, size, letterAt))
    }

    @Test
    fun `prev group goes to the group start, then the previous group`() {
        assertEquals(4, SelectionMath.prevGroup(6, size, letterAt))
        assertEquals(3, SelectionMath.prevGroup(4, size, letterAt))
        assertEquals(1, SelectionMath.prevGroup(3, size, letterAt))
        assertEquals(0, SelectionMath.prevGroup(1, size, letterAt))
        assertEquals(0, SelectionMath.prevGroup(0, size, letterAt))
    }

    @Test
    fun `letter lookup lands on the letter or the next one present`() {
        assertEquals(0, SelectionMath.indexForLetter('#', size, letterAt))
        assertEquals(1, SelectionMath.indexForLetter('A', size, letterAt))
        assertEquals(4, SelectionMath.indexForLetter('C', size, letterAt))
        assertEquals(6, SelectionMath.indexForLetter('Z', size, letterAt))
    }
}

class GridSelectionMathTest {
    // 3 columns, 8 items:
    //   0 1 2
    //   3 4 5
    //   6 7
    private val size = 8
    private val columns = 3

    @Test
    fun `up and down move a row and stay put at the ends`() {
        assertEquals(4, SelectionMath.gridDown(1, size, columns))
        assertEquals(1, SelectionMath.gridUp(4, columns))
        assertEquals(1, SelectionMath.gridUp(1, columns))
        assertEquals(7, SelectionMath.gridDown(7, size, columns))
    }

    @Test
    fun `down into a short last row lands on the last item`() {
        assertEquals(7, SelectionMath.gridDown(5, size, columns))
        assertEquals(6, SelectionMath.gridDown(3, size, columns))
    }

    @Test
    fun `left and right return null at row edges`() {
        assertEquals(null, SelectionMath.gridLeft(3, columns))
        assertEquals(3, SelectionMath.gridLeft(4, columns))
        assertEquals(null, SelectionMath.gridRight(5, size, columns))
        assertEquals(5, SelectionMath.gridRight(4, size, columns))
        assertEquals(null, SelectionMath.gridRight(7, size, columns))
    }

    @Test
    fun `two column grid edges`() {
        assertEquals(null, SelectionMath.gridLeft(2, 2))
        assertEquals(null, SelectionMath.gridRight(3, 10, 2))
        assertEquals(3, SelectionMath.gridRight(2, 10, 2))
    }
}
