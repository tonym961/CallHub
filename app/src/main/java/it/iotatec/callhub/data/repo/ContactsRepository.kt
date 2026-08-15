package it.iotatec.callhub.data.repo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

data class PhoneContact(val name: String, val number: String, val photoUri: String? = null)

/** Loads device contacts (name + phone + photo) from the Contacts provider. */
object ContactsRepository {

    fun load(context: Context): List<PhoneContact> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) return emptyList()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
        )

        val result = LinkedHashMap<String, PhoneContact>()
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        )?.use { c ->
            val nameIdx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoIdx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
            while (c.moveToNext()) {
                val name = c.getString(nameIdx) ?: continue
                val number = c.getString(numIdx)?.replace("\\s".toRegex(), "") ?: continue
                val photo = c.getString(photoIdx)
                // De-duplicate by name+number.
                result.putIfAbsent("$name|$number", PhoneContact(name, number, photo))
            }
        }
        return result.values.toList()
    }
}
