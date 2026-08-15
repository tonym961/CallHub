package it.iotatec.callhub.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat

/** Places outgoing calls through Telecom (cellular by default, or a SIP account). */
object CallPlacer {

    fun canPlaceCalls(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Place a call to [number]. If [accountHandle] is given (e.g. our SIP
     * self-managed account) the call is routed through it; otherwise the user's
     * default calling account (the SIM) is used.
     */
    fun place(context: Context, number: String, accountHandle: PhoneAccountHandle? = null) {
        if (!canPlaceCalls(context) || number.isBlank()) return
        val tm = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val uri = Uri.fromParts("tel", number, null)
        val extras = Bundle().apply {
            accountHandle?.let { putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, it) }
        }
        tm.placeCall(uri, extras)
    }
}
