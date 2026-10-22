package com.zunepocket.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

enum class Accent(val label: String, val color: Color) {
    Magenta("magenta", Color(0xFFEC008C)),
    Orange("orange", Color(0xFFF7941D)),
    Blue("blue", Color(0xFF1BA1E2)),
    Green("lime", Color(0xFF8CBF26)),
    Red("red", Color(0xFFE51400)),
    Purple("purple", Color(0xFFA200FF)),
}

object ZuneColors {
    val Background = Color.Black
    val Primary = Color.White
    val Secondary = Color(0xFF8C8C8C)
    val Dim = Color(0xFF3F3F3F)
    val Scrim = Color(0xEB000000)
}

val LocalAccent = compositionLocalOf { Accent.Magenta.color }
