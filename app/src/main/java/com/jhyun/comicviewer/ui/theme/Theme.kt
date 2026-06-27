package com.jhyun.comicviewer.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8AB4F8),
    background = Color(0xFF101418),
    surface = Color(0xFF16191D),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1F2933),
    background = Color(0xFFF7F7F7),
    surface = Color(0xFFFFFFFF),
)

@Composable
fun ComicViewerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
