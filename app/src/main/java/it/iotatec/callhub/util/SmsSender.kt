package it.iotatec.callhub.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat

/** Sends a quick-reply SMS (used by reject-with-message). */
object SmsSender {

    fun canSend(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
            PackageManager.PERMISSION_GRANTED

    fun send(context: Context, number: String, text: String) {
        if (number.isBlank() || !canSend(context)) return
        val sms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
        runCatching { sms?.sendTextMessage(number, null, text, null, null) }
    }
}
