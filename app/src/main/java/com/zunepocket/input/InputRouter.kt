package com.zunepocket.input

import android.os.Handler
import android.os.Looper
import android.view.KeyEvent

fun interface ActionHandler {
    /** Returns true if the action was consumed. */
    fun onAction(action: AppAction): Boolean
}

/**
 * Single entry point for every physical key press.
 *
 * Handlers form a stack: the most recently registered (the visible screen, or an overlay on top of it)
 * sees each action first; unconsumed actions fall through to older handlers (the global ones).
 *
 * Auto-repeat is generated here rather than relying on the system, because gamepad buttons do not
 * repeat consistently across devices.
 */
class InputRouter(private val bindings: KeyBindings) {

    private val handlers = mutableListOf<ActionHandler>()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var repeatingKeyCode = -1
    private var repeatingAction: AppAction? = null
    private val repeatRunnable = object : Runnable {
        override fun run() {
            val action = repeatingAction ?: return
            dispatch(action)
            mainHandler.postDelayed(this, REPEAT_INTERVAL_MS)
        }
    }

    /** When set, receives every raw key event before mapping (used by the key tester screen). */
    var rawListener: ((KeyEvent) -> Boolean)? = null

    fun push(handler: ActionHandler) {
        handlers.add(handler)
    }

    fun remove(handler: ActionHandler) {
        handlers.remove(handler)
    }

    fun onKeyEvent(event: KeyEvent): Boolean {
        rawListener?.let { listener ->
            cancelRepeat()
            if (listener(event)) return true
        }
        val action = bindings.map(event.keyCode) ?: return false
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                // System-generated repeats are ignored; we run our own timer instead.
                if (event.repeatCount > 0) return true
                cancelRepeat()
                dispatch(action)
                if (bindings.repeats(action)) {
                    repeatingKeyCode = event.keyCode
                    repeatingAction = action
                    mainHandler.postDelayed(repeatRunnable, REPEAT_DELAY_MS)
                }
            }
            KeyEvent.ACTION_UP -> if (event.keyCode == repeatingKeyCode) cancelRepeat()
        }
        return true
    }

    fun dispatch(action: AppAction): Boolean {
        for (handler in handlers.toList().asReversed()) {
            if (handler.onAction(action)) return true
        }
        return false
    }

    fun cancelRepeat() {
        mainHandler.removeCallbacks(repeatRunnable)
        repeatingKeyCode = -1
        repeatingAction = null
    }

    private companion object {
        const val REPEAT_DELAY_MS = 380L
        const val REPEAT_INTERVAL_MS = 70L
    }
}
