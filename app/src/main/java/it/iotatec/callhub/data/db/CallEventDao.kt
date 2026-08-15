package it.iotatec.callhub.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CallEventDao {

    /** Ignore on conflict so re-syncing/re-posting the same event is a no-op. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: CallEventEntity): Long

    @Update
    suspend fun update(event: CallEventEntity)

    @Query("SELECT * FROM call_events ORDER BY startTime DESC LIMIT :limit")
    fun observeRecent(limit: Int = 500): Flow<List<CallEventEntity>>

    @Query("SELECT * FROM call_events WHERE dedupeKey = :key LIMIT 1")
    suspend fun findByDedupeKey(key: String): CallEventEntity?

    @Query("SELECT MAX(startTime) FROM call_events WHERE source = 'PHONE'")
    suspend fun latestNativeStartTime(): Long?

    @Query("UPDATE call_events SET note = :note WHERE id = :id")
    suspend fun updateNote(id: Long, note: String?)
}
