package it.iotatec.callhub.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

private val Green = Color(0xFF0B6E4F)

private val LightColors = lightColorScheme(primary = Green)
private val DarkColors = darkColorScheme(primary = Color(0xFF3DDC97))

@Composable
fun CallHubTheme(content: @Composable () -> Unit) {
    val mode by AppTheme.mode.collectAsState()
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content
    )
}
