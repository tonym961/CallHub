package it.iotatec.callhub.sip

import android.util.Log

/**
 * Placeholder engine: no real SIP signaling or audio. It reports "registered"
 * and drives outgoing-call callbacks so the Telecom/UI plumbing can be exercised
 * end-to-end. Replace with a Linphone/PJSIP-backed implementation for real calls.
 */
object StubSipEngine : SipEngine {

    private const val TAG = "StubSipEngine"

    @Volatile
    override var isRegistered: Boolean = false
        private set

    private var callbacks: SipEngine.CallCallbacks? = null

    override fun register(account: SipAccount, listener: SipEngine.RegistrationListener) {
        Log.i(TAG, "register(${account.id}) — STUB, no real SIP REGISTER sent")
        isRegistered = true
        listener.onRegistrationStateChanged(true, "stub: registrato (nessun stack SIP reale)")
    }

    override fun unregister() {
        isRegistered = false
    }

    override fun startCall(number: String, callbacks: SipEngine.CallCallbacks) {
        Log.i(TAG, "startCall($number) — STUB")
        this.callbacks = callbacks
        callbacks.onRinging()
        callbacks.onConnected()
    }

    override fun answer() {
        callbacks?.onConnected()
    }

    override fun hangup() {
        callbacks?.onEnded()
        callbacks = null
    }

    override fun setMuted(muted: Boolean) {
        Log.i(TAG, "setMuted($muted) — STUB")
    }

    override val hasActiveCall: Boolean get() = callbacks != null
    override val isRecording: Boolean get() = false
    override fun startRecording(filePath: String) { Log.i(TAG, "startRecording($filePath) — STUB") }
    override fun stopRecording() { Log.i(TAG, "stopRecording() — STUB") }
}
