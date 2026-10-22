package com.zunepocket.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.zunepocket.R

/** Selawik: Microsoft's open-source, Segoe-compatible face (SIL OFL 1.1). */
val Selawik = FontFamily(
    Font(R.font.selawik_light, FontWeight.Light),
    Font(R.font.selawik_regular, FontWeight.Normal),
    Font(R.font.selawik_bold, FontWeight.Bold),
)

private fun style(size: TextUnit, weight: FontWeight, tracking: TextUnit = 0.em) = TextStyle(
    fontFamily = Selawik,
    fontWeight = weight,
    fontSize = size,
    lineHeight = size * 1.12f,
    letterSpacing = tracking,
)

/** Sized for the Pocket Classic's ~360x410dp screen. */
object ZuneType {
    val Panorama = style(22.sp, FontWeight.Normal)
    val Pivot = style(46.sp, FontWeight.Light, (-0.02).em)
    val HomeItem = style(48.sp, FontWeight.Light, (-0.02).em)
    val Header = style(38.sp, FontWeight.Light, (-0.02).em)
    val ListPrimary = style(24.sp, FontWeight.Normal)
    val ListSecondary = style(15.sp, FontWeight.Normal)
    val MenuItem = style(30.sp, FontWeight.Light)
    val NowPlayingArtist = style(34.sp, FontWeight.Light, (-0.02).em)
    val NowPlayingTitle = style(24.sp, FontWeight.Normal)
    val Body = style(16.sp, FontWeight.Normal)
    val Caption = style(13.sp, FontWeight.Normal)
    val JumpLetter = style(26.sp, FontWeight.Normal)
}
