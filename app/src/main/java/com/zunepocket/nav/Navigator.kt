package com.zunepocket.nav

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Stable
class Navigator(start: Screen) {
    private val stack = mutableStateListOf(start)

    val current: Screen get() = stack.last()

    /** Whether the most recent change was a pop, so transitions can play in reverse. */
    var lastWasPop by mutableStateOf(false)
        private set

    fun push(screen: Screen) {
        lastWasPop = false
        stack.add(screen)
    }

    fun pop(): Boolean {
        if (stack.size <= 1) return false
        lastWasPop = true
        stack.removeAt(stack.lastIndex)
        return true
    }
}
