package it.iotatec.callhub.dialer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.Call
import androidx.core.app.NotificationCompat
import it.iotatec.callhub.R
import it.iotatec.callhub.ui.InCallActivity

/**
 * Posts the ongoing-call notification. For an incoming (ringing) call it uses a
 * full-screen intent so the in-call screen shows even when the device is locked.
 */
object CallNotifier {

    private const val CHANNEL_ID = "calls"
    private const val NOTIF_ID = 42

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = context.getString(R.string.notif_channel_desc) }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    fun showOngoing(context: Context, call: Call) {
        ensureChannel(context)

        @Suppress("DEPRECATION")
        val isRinging = call.state == Call.STATE_RINGING
        val number = call.details.handle?.schemeSpecificPart ?: context.getString(R.string.unknown_caller)

        val fullScreenIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, InCallActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(if (isRinging) R.string.notif_incoming_title else R.string.notif_ongoing_title))
            .setContentText(number)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(fullScreenIntent)

        if (isRinging) {
            builder.setFullScreenIntent(fullScreenIntent, true)
        }

        context.getSystemService(NotificationManager::class.java)
            .notify(NOTIF_ID, builder.build())
    }

    fun cancel(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(NOTIF_ID)
    }
}
