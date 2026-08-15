package it.iotatec.callhub.voip

import android.content.Context
import it.iotatec.callhub.data.model.CallSource

/**
 * FULL (sideload) flavor.
 *
 * All known messenger packages are monitored by default. This build is meant for
 * personal sideload where the user controls the device and no store policy applies.
 */
object MonitoredPackages {

    const val DISTRIBUTION = "full"
    const val REQUIRES_DISCLOSURE = false

    /** Every messenger CallHub knows how to read (incl. third-party Telegram clients). */
    val AVAILABLE: Set<String> =
        CallSource.entries.mapNotNull { it.packageName }.toSet()

    fun isEnabled(context: Context, pkg: String): Boolean = pkg in AVAILABLE
}
