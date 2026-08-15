package it.iotatec.callhub.voip

import android.app.Notification
import android.app.Person
import android.os.Build
import android.service.notification.StatusBarNotification
import it.iotatec.callhub.data.model.CallDirection

/**
 * Best-effort interpretation of a messenger notification as a call event.
 *
 * VoIP calls never reach the Android telephony stack, so this is heuristic:
 * we rely on Notification.CATEGORY_CALL, the CallStyle template, and localized
 * keyword matching (IT + EN). It yields metadata (who / when / video / missed),
 * never the audio and rarely a phone number.
 */
object VoipCallParser {

    private val MISSED_KEYWORDS = listOf(
        "missed", "persa", "perse", "senza risposta"
    )
    private val VIDEO_KEYWORDS = listOf(
        "video"
    )
    private val INCOMING_KEYWORDS = listOf(
        "incoming", "in arrivo", "chiamata in arrivo", "sta chiamando", "is calling"
    )
    private val ONGOING_KEYWORDS = listOf(
        "ongoing", "in corso", "chiamata in corso", "connected", "connesso", "tap to return"
    )

    data class Parsed(
        val isCallRelated: Boolean,
        val direction: CallDirection,
        val displayName: String?,
        val isVideo: Boolean,
        val isOngoing: Boolean,
        val rawText: String?
    )

    fun parse(sbn: StatusBarNotification): Parsed {
        val n = sbn.notification
        val extras = n.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val template = extras.getString(Notification.EXTRA_TEMPLATE).orEmpty()

        val haystack = listOfNotNull(title, text, bigText)
            .joinToString(" ")
            .lowercase()

        val isCallStyle = template.contains("CallStyle")
        val isCategoryCall = n.category == Notification.CATEGORY_CALL
        val mentionsCall = haystack.contains("call") ||
            haystack.contains("chiamat") ||
            MISSED_KEYWORDS.any { haystack.contains(it) }

        val isCallRelated = isCallStyle || isCategoryCall || mentionsCall

        val isMissed = MISSED_KEYWORDS.any { haystack.contains(it) }
        val isIncoming = INCOMING_KEYWORDS.any { haystack.contains(it) }
        val isOngoing = isCategoryCall || ONGOING_KEYWORDS.any { haystack.contains(it) }

        val direction = when {
            isMissed -> CallDirection.MISSED
            isIncoming || isOngoing -> CallDirection.INCOMING
            else -> CallDirection.UNKNOWN
        }

        // CallStyle carries the caller as a Person; fall back to the title.
        val personName = runCatching {
            val person = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                extras.getParcelable(Notification.EXTRA_CALL_PERSON, Person::class.java)
            } else {
                @Suppress("DEPRECATION")
                extras.getParcelable(Notification.EXTRA_CALL_PERSON) as? Person
            }
            person?.name?.toString()
        }.getOrNull()

        return Parsed(
            isCallRelated = isCallRelated,
            direction = direction,
            displayName = personName ?: title,
            isVideo = VIDEO_KEYWORDS.any { haystack.contains(it) },
            isOngoing = isOngoing && !isMissed,
            rawText = listOfNotNull(title, text).joinToString(" — ").ifBlank { null }
        )
    }
}
