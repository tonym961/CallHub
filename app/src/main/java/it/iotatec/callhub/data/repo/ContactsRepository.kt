package it.iotatec.callhub.data.repo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

/** One row per contact (primary number) for the list. */
data class PhoneContact(
    val id: Long,
    val name: String,
    val number: String,
    val photoUri: String? = null
)

data class ContactNumber(val number: String, val label: String)
data class ContactDetail(val id: Long, val name: String, val photoUri: String?, val numbers: List<ContactNumber>)

/** Loads device contacts (grouped by contact, with all numbers on demand). */
object ContactsRepository {

    private fun granted(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    fun load(context: Context): List<PhoneContact> {
        if (!granted(context)) return emptyList()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
        )

        // Keep one entry per contact (the first number encountered).
        val result = LinkedHashMap<Long, PhoneContact>()
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, null, null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        )?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIdx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoIdx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
            while (c.moveToNext()) {
                val id = c.getLong(idIdx)
                if (result.containsKey(id)) continue
                val name = c.getString(nameIdx) ?: continue
                val number = c.getString(numIdx)?.replace("\\s".toRegex(), "") ?: continue
                result[id] = PhoneContact(id, name, number, c.getString(photoIdx))
            }
        }
        return result.values.toList()
    }

    /** All numbers (with type labels) for one contact. */
    fun loadDetail(context: Context, contactId: Long): ContactDetail? {
        if (!granted(context)) return null
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.LABEL
        )
        var name = ""
        var photo: String? = null
        val numbers = mutableListOf<ContactNumber>()
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection,
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?", arrayOf(contactId.toString()), null
        )?.use { c ->
            val nameIdx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoIdx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
            val typeIdx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE)
            val labelIdx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LABEL)
            val seen = mutableSetOf<String>()
            while (c.moveToNext()) {
                name = c.getString(nameIdx) ?: name
                photo = photo ?: c.getString(photoIdx)
                val number = c.getString(numIdx) ?: continue
                if (!seen.add(number.replace("\\s".toRegex(), ""))) continue
                val label = ContactsContract.CommonDataKinds.Phone
                    .getTypeLabel(context.resources, c.getInt(typeIdx), c.getString(labelIdx)).toString()
                numbers.add(ContactNumber(number, label))
            }
        }
        if (numbers.isEmpty()) return null
        return ContactDetail(contactId, name, photo, numbers)
    }

    /** Contact photo URI for a phone number (for the recents list), if any. */
    fun photoUriForNumber(context: Context, number: String?): String? {
        if (!granted(context) || number.isNullOrBlank()) return null
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.PHOTO_URI), null, null, null)?.use {
            if (it.moveToFirst()) return it.getString(0)
        }
        return null
    }
}

