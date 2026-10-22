package com.zunepocket.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

@Composable
fun ZuneTheme(accent: Color, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalAccent provides accent,
        LocalContentColor provides ZuneColors.Primary,
        LocalTextStyle provides ZuneType.Body,
        content = content,
    )
}
