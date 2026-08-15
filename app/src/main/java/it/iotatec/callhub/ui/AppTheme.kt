package it.iotatec.callhub.ui

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Reactive theme selection, persisted in SharedPreferences. */
object AppTheme {

    private const val PREFS = "theme"
    private const val KEY = "mode"

    private val _mode = MutableStateFlow(ThemeMode.SYSTEM)
    val mode: StateFlow<ThemeMode> = _mode

    fun load(context: Context) {
        val name = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        _mode.value = runCatching { ThemeMode.valueOf(name ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM)
    }

    fun set(context: Context, mode: ThemeMode) {
        _mode.value = mode
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, mode.name).apply()
    }
}
