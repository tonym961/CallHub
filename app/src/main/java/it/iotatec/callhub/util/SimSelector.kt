package it.iotatec.callhub.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat

data class SimAccount(val handle: PhoneAccountHandle, val label: String)

/** Enumerates call-capable phone accounts (SIMs) for dual-SIM selection. */
object SimSelector {

    fun accounts(context: Context): List<SimAccount> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) return emptyList()

        val tm = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        return runCatching {
            tm.callCapablePhoneAccounts.mapNotNull { handle ->
                val account = tm.getPhoneAccount(handle) ?: return@mapNotNull null
                SimAccount(handle, account.label?.toString().orEmpty().ifBlank { handle.id })
            }
        }.getOrDefault(emptyList())
    }
}
