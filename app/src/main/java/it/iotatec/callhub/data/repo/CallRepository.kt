package it.iotatec.callhub.data.repo

import android.content.Context
import android.provider.CallLog
import it.iotatec.callhub.data.db.CallEventDao
import it.iotatec.callhub.data.db.CallEventEntity
import it.iotatec.callhub.data.db.CallHubDatabase
import it.iotatec.callhub.data.model.CallSource
import kotlinx.coroutines.flow.Flow

/**
 * Single entry point for reading/writing unified call events. Both the native
 * CallLog sync and the notification listener funnel through here.
 */
class CallRepository private constructor(private val dao: CallEventDao) {

    fun observeRecent(): Flow<List<CallEventEntity>> = dao.observeRecent()

    suspend fun record(event: CallEventEntity): Long = dao.insert(event)

    suspend fun update(event: CallEventEntity) = dao.update(event)

    suspend fun findByDedupeKey(key: String): CallEventEntity? = dao.findByDedupeKey(key)

    suspend fun latestNativeStartTime(): Long? = dao.latestNativeStartTime()

    suspend fun updateNote(id: Long, note: String?) = dao.updateNote(id, note)

    suspend fun eventsForNumber(number: String): List<CallEventEntity> = dao.eventsForNumber(number)

    /** Delete a call event; for native calls also removes it from the system CallLog. */
    suspend fun delete(context: Context, event: CallEventEntity) {
        if (event.source == CallSource.PHONE && event.phoneNumber != null) {
            runCatching {
                context.contentResolver.delete(
                    CallLog.Calls.CONTENT_URI,
                    "${CallLog.Calls.NUMBER} = ? AND ${CallLog.Calls.DATE} = ?",
                    arrayOf(event.phoneNumber, event.startTime.toString())
                )
            }
        }
        dao.delete(event.id)
    }

    companion object {
        @Volatile private var instance: CallRepository? = null

        fun get(context: Context): CallRepository =
            instance ?: synchronized(this) {
                instance ?: CallRepository(
                    CallHubDatabase.get(context).callEventDao()
                ).also { instance = it }
            }
    }
}
