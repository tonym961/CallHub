package it.iotatec.callhub.dialer

import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UI-facing snapshot of the current call. Recomposed whenever the underlying
 * [Call] changes state or the audio route changes.
 */
data class CallUiState(
    val hasCall: Boolean = false,
    val callState: Int = Call.STATE_NEW,
    val number: String? = null,
    val name: String? = null,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isOnHold: Boolean = false,
    /** Wall-clock time the call connected, or 0 if not yet connected. */
    val connectTimeMillis: Long = 0L
)

/**
 * Bridge between [CallHubInCallService] (system-owned) and the Compose in-call UI.
 * Holds the live [Call] objects and exposes a [StateFlow] the UI observes, plus
 * the actions (answer / hangup / mute / speaker / hold / DTMF) the UI triggers.
 */
object CallManager {

    private var service: InCallService? = null
    private val calls = mutableListOf<Call>()

    private val _state = MutableStateFlow(CallUiState())
    val state: StateFlow<CallUiState> = _state.asStateFlow()

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) = refresh()
        override fun onDetailsChanged(call: Call, details: Call.Details) = refresh()
    }

    fun attach(inCallService: InCallService) { service = inCallService }
    fun detach() { service = null }

    fun add(call: Call) {
        calls.add(call)
        call.registerCallback(callback)
        refresh()
    }

    fun remove(call: Call) {
        call.unregisterCallback(callback)
        calls.remove(call)
        refresh()
    }

    fun hasCalls(): Boolean = calls.isNotEmpty()

    fun onAudioStateChanged(audioState: CallAudioState) {
        _state.value = _state.value.copy(
            isMuted = audioState.isMuted,
            isSpeakerOn = audioState.route == CallAudioState.ROUTE_SPEAKER
        )
    }

    private val primary: Call? get() = calls.lastOrNull()

    fun answer() = primary?.answer(VideoProfile.STATE_AUDIO_ONLY) ?: Unit
    fun reject() = primary?.reject(false, null) ?: Unit
    fun hangup() = primary?.disconnect() ?: Unit

    fun toggleMute() { service?.setMuted(!_state.value.isMuted) }

    @Suppress("DEPRECATION")
    fun toggleSpeaker() {
        service?.setAudioRoute(
            if (_state.value.isSpeakerOn) CallAudioState.ROUTE_EARPIECE
            else CallAudioState.ROUTE_SPEAKER
        )
    }

    fun toggleHold() {
        val c = primary ?: return
        @Suppress("DEPRECATION")
        if (c.state == Call.STATE_HOLDING) c.unhold() else c.hold()
    }

    fun playDtmf(digit: Char) {
        primary?.playDtmfTone(digit)
        primary?.stopDtmfTone()
    }

    private fun refresh() {
        val c = primary
        if (c == null) {
            _state.value = CallUiState()
            return
        }
        val details = c.details
        @Suppress("DEPRECATION")
        val callState = c.state
        _state.value = _state.value.copy(
            hasCall = true,
            callState = callState,
            number = details.handle?.schemeSpecificPart,
            name = details.callerDisplayName,
            isOnHold = callState == Call.STATE_HOLDING,
            connectTimeMillis = details.connectTimeMillis
        )
    }
}
