package it.iotatec.callhub.sip

import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager

/**
 * Telecom entry point for SIP calls. The system calls these when a call is
 * placed/received on our registered SIP [PhoneAccountHandle]; we return a
 * [SipConnection] wired to the active [SipEngine].
 */
class SipConnectionService : ConnectionService() {

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        val connection = SipConnection()
        val address = request?.address
        val number = address?.schemeSpecificPart.orEmpty()
        val accountId = request?.accountHandle?.id ?: connectionManagerPhoneAccount?.id
        connection.setAddress(address, TelecomManager.PRESENTATION_ALLOWED)
        connection.setInitialized()
        SipRegistry.engine.startCall(number, accountId, connection.engineCallbacks)
        return connection
    }

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        val connection = SipConnection()
        val address = request?.address
        connection.setAddress(address, TelecomManager.PRESENTATION_ALLOWED)
        connection.setRinging()
        return connection
    }
}
