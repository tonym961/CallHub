package it.iotatec.callhub.sip

/**
 * Abstraction over the SIP signaling + media stack. The platform SIP API
 * (android.net.sip) was removed in Android 12, so a real implementation must
 * wrap a bundled stack — Linphone SDK (liblinphone, GPLv3/commercial) or PJSIP
 * (pjsua2, GPL/commercial). Implement this interface around the chosen stack and
 * set it via [SipRegistry.engine].
 */
interface SipEngine {

    val isRegistered: Boolean

    fun register(account: SipAccount, listener: RegistrationListener)
    fun unregister()

    /** Begin an outgoing call; drive [callbacks] as signaling progresses. */
    fun startCall(number: String, callbacks: CallCallbacks)

    /** Answer the current incoming call. */
    fun answer()

    /** End the current call (outgoing, incoming, or connected). */
    fun hangup()

    fun setMuted(muted: Boolean)

    /** True while a SIP call is active (used to gate the record button; native calls never set this). */
    val hasActiveCall: Boolean

    val isRecording: Boolean

    /** Record the current SIP call to [filePath] (native-call recording is impossible on Android). */
    fun startRecording(filePath: String)

    fun stopRecording()

    interface RegistrationListener {
        fun onRegistrationStateChanged(registered: Boolean, message: String?)
        /** Engine received an inbound INVITE. */
        fun onIncomingCall(from: String)
    }

    interface CallCallbacks {
        fun onRinging()
        fun onConnected()
        fun onEnded()
    }
}

/** Holds the active engine implementation (swap in a real stack here). */
object SipRegistry {
    @Volatile
    var engine: SipEngine = StubSipEngine
}
