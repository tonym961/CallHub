package it.iotatec.callhub.util

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.telecom.TelecomManager

/** Helpers to request the default-dialer role and open Notification-access settings. */
object DefaultDialerHelper {

    fun isDefaultDialer(context: Context): Boolean {
        val tm = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        return context.packageName == tm.defaultDialerPackage
    }

    /**
     * Builds the intent that asks the user to make CallHub the default dialer.
     * Uses RoleManager on API 29+, falling back to the legacy Telecom action.
     */
    fun requestDefaultDialerIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
            roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
        } else {
            Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
        }
    }

    fun requestDefaultDialer(activity: Activity, requestCode: Int) {
        activity.startActivityForResult(requestDefaultDialerIntent(activity), requestCode)
    }

    /** True once the user has granted "Notification access" to our listener. */
    fun isNotificationAccessGranted(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver, "enabled_notification_listeners"
        ) ?: return false
        return enabled.split(":").any { it.contains(context.packageName) }
    }

    fun notificationAccessSettingsIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
}
