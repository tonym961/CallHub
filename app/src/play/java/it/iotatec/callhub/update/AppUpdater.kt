package it.iotatec.callhub.update

import android.app.Activity

/**
 * PLAY flavor: updates are delivered by Google Play, so in-app APK updating is
 * intentionally a no-op (and would violate Play policy).
 */
object AppUpdater {
    fun checkForUpdates(activity: Activity) { /* no-op: handled by Play Store */ }
}
