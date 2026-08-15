package it.iotatec.callhub.dialer

import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import it.iotatec.callhub.ui.InCallActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Bound by the system when CallHub is the default dialer. Wires each system
 * [Call] to [CallManager] (so the Compose in-call UI can drive it), shows the
 * in-call screen + notification, and re-syncs the native CallLog on disconnect.
 */
class CallHubInCallService : InCallService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        CallManager.attach(this)
    }

    override fun onDestroy() {
        CallManager.detach()
        super.onDestroy()
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallManager.add(call)
        CallNotifier.showOngoing(this, call)
        InCallActivity.start(this)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        CallManager.remove(call)
        if (!CallManager.hasCalls()) CallNotifier.cancel(this)
        // The system writes the CallLog row around disconnect; import it.
        scope.launch { CallLogSync.sync(applicationContext) }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        super.onCallAudioStateChanged(audioState)
        CallManager.onAudioStateChanged(audioState)
    }
}
