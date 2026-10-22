package com.zunepocket.input

import android.view.KeyEvent

/**
 * Maps raw key codes from the Retroid Pocket Classic (and any standard gamepad/keyboard) to [AppAction]s.
 *
 * Retroid firmware can swap A and B system-wide, so which face button confirms is a user setting
 * rather than hard-coded.
 */
class KeyBindings(private val swapConfirmBack: () -> Boolean = { false }) {

    fun map(keyCode: Int): AppAction? = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_UP -> AppAction.Up
        KeyEvent.KEYCODE_DPAD_DOWN -> AppAction.Down
        KeyEvent.KEYCODE_DPAD_LEFT -> AppAction.Left
        KeyEvent.KEYCODE_DPAD_RIGHT -> AppAction.Right

        KeyEvent.KEYCODE_BUTTON_A -> if (swapConfirmBack()) AppAction.Back else AppAction.Confirm
        KeyEvent.KEYCODE_BUTTON_B -> if (swapConfirmBack()) AppAction.Confirm else AppAction.Back
        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> AppAction.Confirm
        KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> AppAction.Back

        KeyEvent.KEYCODE_BUTTON_X -> AppAction.NowPlaying
        KeyEvent.KEYCODE_BUTTON_Y -> AppAction.Context

        KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_PAGE_UP -> AppAction.PageUp
        KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_PAGE_DOWN -> AppAction.PageDown
        KeyEvent.KEYCODE_BUTTON_L2 -> AppAction.PrevGroup
        KeyEvent.KEYCODE_BUTTON_R2 -> AppAction.NextGroup

        KeyEvent.KEYCODE_BUTTON_START,
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
        KeyEvent.KEYCODE_MEDIA_PLAY,
        KeyEvent.KEYCODE_MEDIA_PAUSE,
        KeyEvent.KEYCODE_HEADSETHOOK,
        KeyEvent.KEYCODE_SPACE -> AppAction.PlayPause
        KeyEvent.KEYCODE_MEDIA_NEXT -> AppAction.NextTrack
        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> AppAction.PreviousTrack
        KeyEvent.KEYCODE_BUTTON_SELECT -> AppAction.JumpList

        else -> null
    }

    /** Actions that auto-repeat while the button is held. */
    fun repeats(action: AppAction): Boolean = when (action) {
        AppAction.Up, AppAction.Down, AppAction.Left, AppAction.Right,
        AppAction.PageUp, AppAction.PageDown,
        AppAction.PrevGroup, AppAction.NextGroup -> true
        else -> false
    }
}
