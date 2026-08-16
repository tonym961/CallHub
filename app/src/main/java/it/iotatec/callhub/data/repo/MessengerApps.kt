package it.iotatec.callhub.data.repo

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

/** A "call via WhatsApp/Telegram" action a contact number supports. */
data class MessengerCall(val app: String, val label: String, val dataId: Long, val mimeType: String, val video: Boolean)

/**
 * Detects whether a contact is reachable on WhatsApp / WhatsApp Business / Telegram.
 * Those apps register per-number data rows (with dedicated MIME types) for synced
 * contacts; we read them and launch the call via the data-row Intent.
 */
object MessengerApps {

    private data class Mime(val app: String, val mime: String, val video: Boolean)

    private val MIMES = listOf(
        Mime("WhatsApp", "vnd.android.cursor.item/vnd.com.whatsapp.voip.call", false),
        Mime("WhatsApp", "vnd.android.cursor.item/vnd.com.whatsapp.video.call", true),
        Mime("WhatsApp Business", "vnd.android.cursor.item/vnd.com.whatsapp.w4b.voip.call", false),
        Mime("WhatsApp Business", "vnd.android.cursor.item/vnd.com.whatsapp.w4b.video.call", true),
        Mime("Telegram", "vnd.android.cursor.item/vnd.org.telegram.messenger.android.call", false),
        Mime("Telegram", "vnd.android.cursor.item/vnd.org.telegram.messenger.android.call.video", true)
    )

    fun forContact(context: Context, contactId: Long): List<MessengerCall> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }
        val mimes = MIMES.map { it.mime }
        val placeholders = mimes.joinToString(",") { "?" }
        val selection = "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} IN ($placeholders)"
        val args = (listOf(contactId.toString()) + mimes).toTypedArray()

        val out = mutableListOf<MessengerCall>()
        context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data._ID, ContactsContract.Data.MIMETYPE, ContactsContract.Data.DATA3),
            selection, args, null
        )?.use { c ->
            while (c.moveToNext()) {
                val mime = c.getString(1)
                val m = MIMES.firstOrNull { it.mime == mime } ?: continue
                val label = c.getString(2)?.takeIf { it.isNotBlank() } ?: (m.app + if (m.video) " video" else "")
                out.add(MessengerCall(m.app, label, c.getLong(0), mime, m.video))
            }
        }
        return out
    }

    fun call(context: Context, mc: MessengerCall) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, mc.dataId), mc.mimeType)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }
}
