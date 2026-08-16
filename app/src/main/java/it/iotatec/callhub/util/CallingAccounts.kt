package it.iotatec.callhub.util

import android.content.Context
import android.telecom.PhoneAccountHandle
import it.iotatec.callhub.sip.SipAccountStore
import it.iotatec.callhub.sip.SipManager

/** A calling identity the user can pick from when placing a call (SIM or SIP). */
data class CallingOption(val label: String, val handle: PhoneAccountHandle?)

/** Aggregates the available calling accounts: SIMs + saved SIP account(s). */
object CallingAccounts {

    fun list(context: Context): List<CallingOption> {
        val out = mutableListOf<CallingOption>()
        val seen = mutableSetOf<String>()

        SimSelector.accounts(context).forEach { sim ->
            if (seen.add(sim.handle.id)) out.add(CallingOption(sim.label, sim.handle))
        }
        SipAccountStore.load(context)?.let { acc ->
            val handle = SipManager.phoneAccountHandle(context, acc)
            if (seen.add(handle.id)) out.add(CallingOption(acc.displayName.ifBlank { acc.username }, handle))
        }
        return out
    }
}
