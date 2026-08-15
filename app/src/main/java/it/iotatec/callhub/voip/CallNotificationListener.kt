package it.iotatec.callhub.voip

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import it.iotatec.callhub.data.db.CallEventEntity
import it.iotatec.callhub.data.model.CallDirection
import it.iotatec.callhub.data.model.CallSource
import it.iotatec.callhub.data.repo.CallRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Detects WhatsApp / WhatsApp Business / Telegram (and third-party Telegram
 * clients) calls by watching their notifications.
 *
 * Model: an ongoing-call notification appearing = call started; its removal =
 * call ended → we compute an approximate duration. A "missed" notification is
 * recorded directly. Which packages are watched comes from the flavor-specific
 * [MonitoredPackages] (full = all by default, play = opt-in + disclosure).
 *
 * Limitations (documented, not bugs): duration is approximate, direction is
 * heuristic, phone numbers are usually absent, and active-call state held here
 * is in-memory so a service restart can drop an in-flight call.
 */
class CallNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val repo by lazy { CallRepository.get(applicationContext) }

    private data class ActiveCall(
        val source: CallSource,
        val pkg: String,
        val displayName: String?,
        val isVideo: Boolean,
        val startTime: Long,
        val rawText: String?
    )

    // Keyed by the notification key so we can match posted → removed.
    private val active = ConcurrentHashMap<String, ActiveCall>()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!MonitoredPackages.isEnabled(applicationContext, sbn.packageName)) return

        val parsed = VoipCallParser.parse(sbn)
        if (!parsed.isCallRelated) return

        val source = CallSource.fromPackage(sbn.packageName)

        if (parsed.direction == CallDirection.MISSED) {
            recordMissed(sbn, source, parsed)
            return
        }

        if (parsed.isOngoing) {
            active[sbn.key] = ActiveCall(
                source = source,
                pkg = sbn.packageName,
                displayName = parsed.displayName,
                isVideo = parsed.isVideo,
                startTime = sbn.postTime,
                rawText = parsed.rawText
            )
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        val call = active.remove(sbn.key) ?: return
        val endTime = System.currentTimeMillis()
        val durationSec = ((endTime - call.startTime) / 1000).coerceAtLeast(0)

        // Very short "ongoing" notifications are usually unanswered → treat as missed.
        val direction = if (durationSec < 2) CallDirection.MISSED else CallDirection.INCOMING

        scope.launch {
            repo.record(
                CallEventEntity(
                    source = call.source,
                    sourcePackage = call.pkg,
                    direction = direction,
                    displayName = call.displayName,
                    phoneNumber = null,
                    startTime = call.startTime,
                    endTime = endTime,
                    durationSec = durationSec,
                    isVideo = call.isVideo,
                    rawText = call.rawText,
                    dedupeKey = "${call.pkg}:${call.startTime}"
                )
            )
        }
    }

    private fun recordMissed(
        sbn: StatusBarNotification,
        source: CallSource,
        parsed: VoipCallParser.Parsed
    ) {
        scope.launch {
            repo.record(
                CallEventEntity(
                    source = source,
                    sourcePackage = sbn.packageName,
                    direction = CallDirection.MISSED,
                    displayName = parsed.displayName,
                    phoneNumber = null,
                    startTime = sbn.postTime,
                    endTime = null,
                    durationSec = null,
                    isVideo = parsed.isVideo,
                    rawText = parsed.rawText,
                    dedupeKey = "${sbn.packageName}:missed:${sbn.postTime}"
                )
            )
        }
    }
}
