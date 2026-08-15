package it.iotatec.callhub.voip

import android.content.Context
import it.iotatec.callhub.data.model.CallSource

/**
 * PLAY (store) flavor.
 *
 * Reading other apps' call notifications is the part Google reviews most closely,
 * so nothing is monitored until the user explicitly opts in per app, after a
 * prominent in-app disclosure. Default state = everything OFF.
 */
object MonitoredPackages {

    const val DISTRIBUTION = "play"
    const val REQUIRES_DISCLOSURE = true

    private const val PREFS = "voip_optin"

    /** Apps the user may choose to enable (none active by default). */
    val AVAILABLE: Set<String> =
        CallSource.entries.mapNotNull { it.packageName }.toSet()

    fun isEnabled(context: Context, pkg: String): Boolean {
        if (pkg !in AVAILABLE) return false
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(pkg, false)
    }

    fun setEnabled(context: Context, pkg: String, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(pkg, enabled)
            .apply()
    }
}
