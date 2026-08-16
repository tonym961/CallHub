package it.iotatec.callhub.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Reject-with-message: opens the device's default SMS app with the reply
 * pre-filled. We deliberately avoid the SEND_SMS permission — it is a restricted
 * permission that a non-default-SMS, sideloaded app cannot be granted, and it is
 * a Play-policy liability. The user just taps send in their SMS app.
 */
object SmsSender {

    /** An SMS app is virtually always present; the intent handles the rest. */
    fun canSend(context: Context): Boolean = true

    fun send(context: Context, number: String, text: String) {
        if (number.isBlank()) return
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number")).apply {
            putExtra("sms_body", text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}
