package it.iotatec.callhub.dialer.spam

import android.content.ContentValues
import android.content.Context
import android.provider.BlockedNumberContract

/**
 * System-level number blocking via [BlockedNumberContract]. Works only when
 * CallHub is the default dialer/SMS app; every call is guarded and degrades to a
 * no-op otherwise (the local [SpamRepository] remains the fallback).
 */
object SystemBlocklist {

    fun canUse(context: Context): Boolean =
        runCatching { BlockedNumberContract.canCurrentUserBlockNumbers(context) }.getOrDefault(false)

    fun isBlocked(context: Context, number: String?): Boolean {
        if (number.isNullOrBlank()) return false
        return runCatching { BlockedNumberContract.isBlocked(context, number) }.getOrDefault(false)
    }

    fun block(context: Context, number: String) {
        runCatching {
            val values = ContentValues().apply {
                put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, number)
            }
            context.contentResolver.insert(BlockedNumberContract.BlockedNumbers.CONTENT_URI, values)
        }
    }

    fun unblock(context: Context, number: String) {
        runCatching {
            context.contentResolver.delete(
                BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                "${BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER} = ?",
                arrayOf(number)
            )
        }
    }
}
