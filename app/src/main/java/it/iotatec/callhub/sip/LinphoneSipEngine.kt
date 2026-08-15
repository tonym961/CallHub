package it.iotatec.callhub.sip

import android.content.Context
import android.util.Log
import org.linphone.core.Account
import org.linphone.core.Call
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.Factory
import org.linphone.core.RegistrationState

/**
 * Real SIP engine backed by liblinphone (GPLv3). Creates a [Core], registers a
 * SIP account, and bridges call state to the Telecom-facing [SipEngine] contract.
 *
 * Auto-iterate is enabled by default on Android, so no manual iterate loop is
 * required. Requires INTERNET + RECORD_AUDIO permissions.
 */
class LinphoneSipEngine(context: Context) : SipEngine {

    private val core: Core = Factory.instance().createCore(null, null, context.applicationContext)

    private var regListener: SipEngine.RegistrationListener? = null
    private var callCallbacks: SipEngine.CallCallbacks? = null
    private var currentCall: Call? = null

    @Volatile
    override var isRegistered: Boolean = false
        private set

    private val listener = object : CoreListenerStub() {
        override fun onAccountRegistrationStateChanged(
            core: Core,
            account: Account,
            state: RegistrationState?,
            message: String
        ) {
            when (state) {
                RegistrationState.Ok -> { isRegistered = true; regListener?.onRegistrationStateChanged(true, message) }
                RegistrationState.Failed, RegistrationState.Cleared, RegistrationState.None ->
                    { isRegistered = false; regListener?.onRegistrationStateChanged(false, message) }
                else -> { /* Progress */ }
            }
        }

        override fun onCallStateChanged(
            core: Core,
            call: Call,
            state: Call.State?,
            message: String
        ) {
            when (state) {
                Call.State.IncomingReceived -> {
                    currentCall = call
                    val from = call.remoteAddress.username ?: call.remoteAddress.asStringUriOnly()
                    regListener?.onIncomingCall(from)
                }
                Call.State.OutgoingProgress, Call.State.OutgoingRinging -> callCallbacks?.onRinging()
                Call.State.Connected, Call.State.StreamsRunning -> callCallbacks?.onConnected()
                Call.State.End, Call.State.Released, Call.State.Error -> {
                    callCallbacks?.onEnded()
                    currentCall = null
                }
                else -> { /* other states */ }
            }
        }
    }

    init {
        core.addListener(listener)
        core.start()
    }

    override fun register(account: SipAccount, listener: SipEngine.RegistrationListener) {
        regListener = listener
        val factory = Factory.instance()

        val authInfo = factory.createAuthInfo(
            account.username, null, account.password, null, null, account.domain
        )
        core.addAuthInfo(authInfo)

        val params = core.createAccountParams()
        params.identityAddress = factory.createAddress("sip:${account.username}@${account.domain}")

        val transport = when (account.transport) {
            SipAccount.Transport.TCP -> ";transport=tcp"
            SipAccount.Transport.TLS -> ";transport=tls"
            SipAccount.Transport.UDP -> ""
        }
        params.serverAddress = factory.createAddress("sip:${account.domain}:${account.port}$transport")
        params.isRegisterEnabled =true

        val acc = core.createAccount(params)
        core.addAccount(acc)
        core.defaultAccount = acc
        Log.i(TAG, "Registering ${account.id}")
    }

    override fun unregister() {
        core.defaultAccount?.let { acc ->
            val params = acc.params.clone()
            params.isRegisterEnabled =false
            acc.params = params
        }
        isRegistered = false
    }

    override fun startCall(number: String, callbacks: SipEngine.CallCallbacks) {
        callCallbacks = callbacks
        val domain = core.defaultAccount?.params?.identityAddress?.domain
        val uri = if (number.contains("@")) "sip:$number" else "sip:$number@$domain"
        val address = core.interpretUrl(uri)
        if (address == null) {
            callbacks.onEnded()
            return
        }
        currentCall = core.inviteAddress(address)
    }

    override fun answer() {
        currentCall?.accept()
    }

    override fun hangup() {
        currentCall?.terminate()
        currentCall = null
    }

    override fun setMuted(muted: Boolean) {
        core.isMicEnabled = !muted
    }

    companion object {
        private const val TAG = "LinphoneSipEngine"
    }
}
