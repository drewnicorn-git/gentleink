package com.gentleink.reader.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF2F6F5E),
    secondary = Color(0xFF5C8A78),
    background = Color(0xFFF7F5F0),
    surface = Color(0xFFFFFFFF)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8BCBB5),
    secondary = Color(0xFF5C8A78),
    background = Color(0xFF121412),
    surface = Color(0xFF1C211E)
)

@Composable
fun GentleInkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
