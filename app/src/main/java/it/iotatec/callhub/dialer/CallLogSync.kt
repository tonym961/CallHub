package it.iotatec.callhub.dialer

import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import androidx.core.content.ContextCompat
import it.iotatec.callhub.data.db.CallEventEntity
import it.iotatec.callhub.data.model.CallDirection
import it.iotatec.callhub.data.model.CallSource
import it.iotatec.callhub.data.repo.CallRepository

/**
 * Imports native cellular calls from the system [CallLog] into the unified store.
 * Only inserts rows newer than the most recent native event already stored.
 */
object CallLogSync {

    suspend fun sync(context: Context) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val repo = CallRepository.get(context)

        val projection = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION
        )

        // Read the whole log and let the unique dedupeKey drop already-imported
        // rows (INSERT IGNORE). Cheap even for large logs and robust to
        // back-dated / out-of-order entries.
        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            null,
            null,
            "${CallLog.Calls.DATE} DESC"
        )?.use { cursor ->
            val numberIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val nameIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
            val typeIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
            val dateIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
            val durIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)

            while (cursor.moveToNext()) {
                val number = cursor.getString(numberIdx)
                val name = cursor.getString(nameIdx)
                val type = cursor.getInt(typeIdx)
                val date = cursor.getLong(dateIdx)
                val durationSec = cursor.getLong(durIdx)

                val direction = when (type) {
                    CallLog.Calls.INCOMING_TYPE -> CallDirection.INCOMING
                    CallLog.Calls.OUTGOING_TYPE -> CallDirection.OUTGOING
                    CallLog.Calls.MISSED_TYPE -> CallDirection.MISSED
                    CallLog.Calls.REJECTED_TYPE -> CallDirection.REJECTED
                    else -> CallDirection.UNKNOWN
                }

                repo.record(
                    CallEventEntity(
                        source = CallSource.PHONE,
                        sourcePackage = null,
                        direction = direction,
                        displayName = name,
                        phoneNumber = number,
                        startTime = date,
                        endTime = date + durationSec * 1000,
                        durationSec = durationSec,
                        isVideo = false,
                        rawText = null,
                        dedupeKey = "PHONE:${number ?: "?"}:$date"
                    )
                )
            }
        }
    }
}
