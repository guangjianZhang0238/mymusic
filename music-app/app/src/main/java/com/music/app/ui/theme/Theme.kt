package com.music.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun MusicAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    primaryColor: Color = if (darkTheme) MdThemeDarkPrimary else MdThemeLightPrimary,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DynamicColorScheme.generateDarkColorScheme(primaryColor)
    } else {
        DynamicColorScheme.generateLightColorScheme(primaryColor)
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
