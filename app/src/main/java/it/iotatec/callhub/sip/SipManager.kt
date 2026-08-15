package it.iotatec.callhub.sip

import android.content.ComponentName
import android.content.Context
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Registers the SIP account with Android Telecom as a managed call-provider
 * [PhoneAccount], so SIP calls are handled by our default-dialer InCallService
 * (same in-call UI) and written to the system CallLog.
 *
 * Note: a call-provider account must be enabled by the user under
 * Settings → Calls → Calling accounts before it can place calls.
 */
object SipManager {

    private val _registered = MutableStateFlow(false)
    val registered: StateFlow<Boolean> = _registered

    private val _status = MutableStateFlow("Non registrato")
    val status: StateFlow<String> = _status

    fun phoneAccountHandle(context: Context, account: SipAccount): PhoneAccountHandle =
        PhoneAccountHandle(
            ComponentName(context, SipConnectionService::class.java),
            account.id
        )

    fun registerPhoneAccount(context: Context, account: SipAccount) {
        val tm = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val handle = phoneAccountHandle(context, account)
        val phoneAccount = PhoneAccount.builder(handle, account.displayName)
            .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER)
            .addSupportedUriScheme(PhoneAccount.SCHEME_TEL)
            .addSupportedUriScheme(PhoneAccount.SCHEME_SIP)
            .build()
        tm.registerPhoneAccount(phoneAccount)

        SipRegistry.engine.register(account, object : SipEngine.RegistrationListener {
            override fun onRegistrationStateChanged(registered: Boolean, message: String?) {
                _registered.value = registered
                _status.value = message ?: if (registered) "Registrato" else "Non registrato"
            }

            override fun onIncomingCall(from: String) {
                val extras = android.os.Bundle().apply {
                    putParcelable(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS,
                        android.net.Uri.fromParts(PhoneAccount.SCHEME_SIP, from, null))
                }
                tm.addNewIncomingCall(handle, extras)
            }
        })
    }

    fun unregister(context: Context, account: SipAccount) {
        val tm = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        SipRegistry.engine.unregister()
        tm.unregisterPhoneAccount(phoneAccountHandle(context, account))
        _registered.value = false
        _status.value = "Non registrato"
    }
}
