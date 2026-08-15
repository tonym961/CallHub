package it.iotatec.callhub.data.repo

import android.content.Context
import it.iotatec.callhub.data.db.CallEventDao
import it.iotatec.callhub.data.db.CallEventEntity
import it.iotatec.callhub.data.db.CallHubDatabase
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
