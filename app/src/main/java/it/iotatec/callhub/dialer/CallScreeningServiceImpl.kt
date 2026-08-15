package it.iotatec.callhub.dialer

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import androidx.core.content.ContextCompat
import it.iotatec.callhub.dialer.spam.Reputation
import it.iotatec.callhub.dialer.spam.ReputationProvider
import it.iotatec.callhub.dialer.spam.SpamRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Screens every incoming call against the local spam policy (blocklist +
 * anonymous / non-contact rules) and the pluggable reputation provider.
 */
class CallScreeningServiceImpl : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart
        val isAnonymous = number.isNullOrBlank()

        val block = shouldBlock(number, isAnonymous)

        val response = CallResponse.Builder()
            .setDisallowCall(block)
            .setRejectCall(block)
            .setSkipCallLog(false)
            .setSkipNotification(block)
            .build()
        respondToCall(callDetails, response)
    }

    private fun shouldBlock(number: String?, isAnonymous: Boolean): Boolean {
        if (isAnonymous) return SpamRepository.blockAnonymous(this)

        if (SpamRepository.isBlocked(this, number)) return true

        if (SpamRepository.blockNonContacts(this) && !isInContacts(number)) return true

        // Online reputation (default provider is a no-op). Bounded so we never ANR.
        val verdict = runBlocking {
            withTimeoutOrNull(1500) { Reputation.provider.lookup(number!!) }
        }
        return verdict == ReputationProvider.Verdict.SPAM
    }

    private fun isInContacts(number: String?): Boolean {
        if (number.isNullOrBlank()) return false
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) return true // Can't verify → don't block on this rule.

        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number)
        )
        contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)
            ?.use { return it.moveToFirst() }
        return false
    }
}
