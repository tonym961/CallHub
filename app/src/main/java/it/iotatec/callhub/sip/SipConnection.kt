package it.iotatec.callhub.sip

import android.telecom.Connection
import android.telecom.DisconnectCause

/**
 * A single SIP call as seen by Android Telecom. Delegates the user's in-call
 * actions to the [SipEngine] and reflects engine signaling back as Telecom states.
 */
class SipConnection : Connection() {

    init {
        setAudioModeIsVoip(true)
        connectionCapabilities = CAPABILITY_MUTE or CAPABILITY_SUPPORT_HOLD or CAPABILITY_HOLD
    }

    val engineCallbacks = object : SipEngine.CallCallbacks {
        override fun onRinging() { setDialing() }
        override fun onConnected() { setActive() }
        override fun onEnded() {
            setDisconnected(DisconnectCause(DisconnectCause.REMOTE))
            destroy()
        }
    }

    override fun onAnswer() {
        SipRegistry.engine.answer()
        setActive()
    }

    override fun onReject() {
        SipRegistry.engine.hangup()
        setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
        destroy()
    }

    override fun onDisconnect() {
        SipRegistry.engine.hangup()
        setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
        destroy()
    }

    override fun onAbort() {
        SipRegistry.engine.hangup()
        setDisconnected(DisconnectCause(DisconnectCause.CANCELED))
        destroy()
    }

    override fun onHold() { setOnHold() }
    override fun onUnhold() { setActive() }
}
