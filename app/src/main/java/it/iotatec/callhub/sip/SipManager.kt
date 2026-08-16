package it.iotatec.callhub.sip

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Registers SIP accounts with Android Telecom as managed call-provider
 * [PhoneAccount]s (one per account), so SIP calls use the default-dialer in-call
 * UI and land in the CallLog. Supports multiple accounts.
 *
 * A call-provider account must be enabled by the user under
 * Settings → Calls → Calling accounts before it can place calls.
 */
object SipManager {

    private val _registered = MutableStateFlow(false)
    val registered: StateFlow<Boolean> = _registered

    private val _status = MutableStateFlow("Non registrato")
    val status: StateFlow<String> = _status

    fun phoneAccountHandle(context: Context, account: SipAccount): PhoneAccountHandle =
        PhoneAccountHandle(ComponentName(context, SipConnectionService::class.java), account.id)

    private fun registerPhoneAccountFor(context: Context, account: SipAccount) {
        val tm = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val phoneAccount = PhoneAccount.builder(phoneAccountHandle(context, account), account.displayName.ifBlank { account.username })
            .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER)
            .addSupportedUriScheme(PhoneAccount.SCHEME_TEL)
            .addSupportedUriScheme(PhoneAccount.SCHEME_SIP)
            .build()
        tm.registerPhoneAccount(phoneAccount)
    }

    /** Register every saved SIP account (Telecom + engine). */
    fun registerAll(context: Context) {
        val accounts = SipAccountStore.loadAll(context)
        if (accounts.isEmpty()) return
        accounts.forEach { registerPhoneAccountFor(context, it) }

        val tm = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        SipRegistry.engine.register(accounts, object : SipEngine.RegistrationListener {
            override fun onRegistrationStateChanged(registered: Boolean, message: String?) {
                _registered.value = registered
                _status.value = message ?: if (registered) "Registrato" else "Non registrato"
            }

            override fun onIncomingCall(from: String) {
                val account = SipAccountStore.loadAll(context).firstOrNull() ?: return
                val extras = Bundle().apply {
                    putParcelable(
                        TelecomManager.EXTRA_INCOMING_CALL_ADDRESS,
                        Uri.fromParts(PhoneAccount.SCHEME_SIP, from, null)
                    )
                }
                tm.addNewIncomingCall(phoneAccountHandle(context, account), extras)
            }
        })
    }

    /** Save + (re)register a single account. */
    fun registerAccount(context: Context, account: SipAccount) {
        SipAccountStore.addOrUpdate(context, account)
        registerAll(context)
    }

    fun removeAccount(context: Context, account: SipAccount) {
        val tm = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        tm.unregisterPhoneAccount(phoneAccountHandle(context, account))
        SipAccountStore.remove(context, account.id)
        if (SipAccountStore.loadAll(context).isEmpty()) {
            SipRegistry.engine.unregister()
            _registered.value = false
            _status.value = "Non registrato"
        }
    }
}
