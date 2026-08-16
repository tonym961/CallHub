package it.iotatec.callhub.security

import android.content.Context

/** Whether the app requires biometric / device-credential unlock on launch. */
object AppLock {

    private const val PREFS = "security"
    private const val KEY = "app_lock"

    /** Unlocked for the current process lifetime (reset when the app is killed). */
    @Volatile
    var unlockedThisSession = false

    fun enabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false)

    fun setEnabled(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, value).apply()
    }
}
