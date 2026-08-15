package it.iotatec.callhub.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import it.iotatec.callhub.data.model.CallDirection
import it.iotatec.callhub.data.model.CallSource

/**
 * One unified call record, from either the native CallLog or an app notification.
 *
 * [dedupeKey] carries a stable identity so re-syncing the CallLog or a re-posted
 * notification does not create duplicates (see the unique index below).
 */
@Entity(
    tableName = "call_events",
    indices = [Index(value = ["dedupeKey"], unique = true)]
)
data class CallEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val source: CallSource,
    val sourcePackage: String?,
    val direction: CallDirection,
    val displayName: String?,
    /** Usually only available for native calls; VoIP notifications rarely expose it. */
    val phoneNumber: String?,
    val startTime: Long,
    val endTime: Long?,
    val durationSec: Long?,
    val isVideo: Boolean,
    /** Raw notification title/text kept for debugging heuristics; not shown in the UI. */
    val rawText: String?,
    val dedupeKey: String,
    /** Optional user note attached to this call. */
    val note: String? = null
)
