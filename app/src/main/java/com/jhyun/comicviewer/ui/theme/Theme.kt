package com.jhyun.comicviewer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 참고 앱(Perfect Viewer 계열) 톤: 중성 다크 그레이 배경 + 블루 강조.
private val DarkColors =
    darkColorScheme(
        primary = Color(0xFF2196F3),
        onPrimary = Color(0xFFFFFFFF),
        background = Color(0xFF2B2B2B),
        onBackground = Color(0xFFEDEDED),
        surface = Color(0xFF333333),
        onSurface = Color(0xFFEDEDED),
        surfaceVariant = Color(0xFF3A3A3A),
        onSurfaceVariant = Color(0xFFB0B0B0),
    )

private val LightColors =
    lightColorScheme(
        primary = Color(0xFF1F2933),
        background = Color(0xFFF7F7F7),
        surface = Color(0xFFFFFFFF),
    )

@Composable
fun ComicViewerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
