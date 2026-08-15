package it.iotatec.callhub.ui

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Reactive theme selection (mode + accent color), persisted in SharedPreferences. */
object AppTheme {

    private const val PREFS = "theme"
    private const val KEY_MODE = "mode"
    private const val KEY_ACCENT = "accent"

    const val DEFAULT_ACCENT = 0xFF0B6E4FL

    /** Preset accent colors (ARGB). */
    val ACCENT_PRESETS = listOf(
        0xFF0B6E4FL, // green
        0xFF1565C0L, // blue
        0xFF6A1B9AL, // purple
        0xFFC62828L, // red
        0xFFEF6C00L, // orange
        0xFF00838FL  // teal
    )

    private val _mode = MutableStateFlow(ThemeMode.SYSTEM)
    val mode: StateFlow<ThemeMode> = _mode

    private val _accent = MutableStateFlow(DEFAULT_ACCENT)
    val accent: StateFlow<Long> = _accent

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _mode.value = runCatching { ThemeMode.valueOf(prefs.getString(KEY_MODE, null) ?: "SYSTEM") }
            .getOrDefault(ThemeMode.SYSTEM)
        _accent.value = prefs.getLong(KEY_ACCENT, DEFAULT_ACCENT)
    }

    fun set(context: Context, mode: ThemeMode) {
        _mode.value = mode
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_MODE, mode.name).apply()
    }

    fun setAccent(context: Context, argb: Long) {
        _accent.value = argb
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong(KEY_ACCENT, argb).apply()
    }
}
