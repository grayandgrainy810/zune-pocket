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
