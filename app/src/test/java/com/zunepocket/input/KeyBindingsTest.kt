package com.zunepocket.input

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyBindingsTest {

    @Test
    fun `face and shoulder buttons map to their default actions`() {
        val bindings = KeyBindings()
        assertEquals(AppAction.Confirm, bindings.map(KeyEvent.KEYCODE_BUTTON_A))
        assertEquals(AppAction.Back, bindings.map(KeyEvent.KEYCODE_BUTTON_B))
        assertEquals(AppAction.NowPlaying, bindings.map(KeyEvent.KEYCODE_BUTTON_X))
        assertEquals(AppAction.Context, bindings.map(KeyEvent.KEYCODE_BUTTON_Y))
        assertEquals(AppAction.PageUp, bindings.map(KeyEvent.KEYCODE_BUTTON_L1))
        assertEquals(AppAction.PageDown, bindings.map(KeyEvent.KEYCODE_BUTTON_R1))
        assertEquals(AppAction.PrevGroup, bindings.map(KeyEvent.KEYCODE_BUTTON_L2))
        assertEquals(AppAction.NextGroup, bindings.map(KeyEvent.KEYCODE_BUTTON_R2))
        assertEquals(AppAction.PlayPause, bindings.map(KeyEvent.KEYCODE_BUTTON_START))
        assertEquals(AppAction.JumpList, bindings.map(KeyEvent.KEYCODE_BUTTON_SELECT))
        assertEquals(AppAction.Back, bindings.map(KeyEvent.KEYCODE_BACK))
    }

    @Test
    fun `d-pad maps to directions`() {
        val bindings = KeyBindings()
        assertEquals(AppAction.Up, bindings.map(KeyEvent.KEYCODE_DPAD_UP))
        assertEquals(AppAction.Down, bindings.map(KeyEvent.KEYCODE_DPAD_DOWN))
        assertEquals(AppAction.Left, bindings.map(KeyEvent.KEYCODE_DPAD_LEFT))
        assertEquals(AppAction.Right, bindings.map(KeyEvent.KEYCODE_DPAD_RIGHT))
    }

    @Test
    fun `swap setting exchanges A and B only`() {
        var swapped = false
        val bindings = KeyBindings { swapped }
        swapped = true
        assertEquals(AppAction.Back, bindings.map(KeyEvent.KEYCODE_BUTTON_A))
        assertEquals(AppAction.Confirm, bindings.map(KeyEvent.KEYCODE_BUTTON_B))
        assertEquals(AppAction.Back, bindings.map(KeyEvent.KEYCODE_BACK))
        swapped = false
        assertEquals(AppAction.Confirm, bindings.map(KeyEvent.KEYCODE_BUTTON_A))
    }

    @Test
    fun `volume keys are left to the system`() {
        val bindings = KeyBindings()
        assertNull(bindings.map(KeyEvent.KEYCODE_VOLUME_UP))
        assertNull(bindings.map(KeyEvent.KEYCODE_VOLUME_DOWN))
    }

    @Test
    fun `only movement actions auto-repeat`() {
        val bindings = KeyBindings()
        assertTrue(bindings.repeats(AppAction.Down))
        assertTrue(bindings.repeats(AppAction.NextGroup))
        assertFalse(bindings.repeats(AppAction.Confirm))
        assertFalse(bindings.repeats(AppAction.PlayPause))
        assertFalse(bindings.repeats(AppAction.Back))
    }
}
