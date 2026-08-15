package it.iotatec.callhub.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

@Composable
fun CallHubTheme(content: @Composable () -> Unit) {
    val mode by AppTheme.mode.collectAsState()
    val accent by AppTheme.accent.collectAsState()
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val primary = Color(accent)
    val colors = if (dark) darkColorScheme(primary = primary) else lightColorScheme(primary = primary)
    MaterialTheme(colorScheme = colors, content = content)
}
