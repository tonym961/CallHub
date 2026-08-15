package it.iotatec.callhub.dialer.spam

import android.content.Context

/**
 * Local, zero-cost spam policy: a user-managed blocklist plus simple rules
 * (block hidden/anonymous numbers, block numbers not in contacts). Persisted in
 * SharedPreferences. Online reputation is layered on top via [ReputationProvider].
 */
object SpamRepository {

    private const val PREFS = "spam_policy"
    private const val KEY_BLOCKED = "blocked_numbers"
    private const val KEY_BLOCK_ANON = "block_anonymous"
    private const val KEY_BLOCK_NON_CONTACTS = "block_non_contacts"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun blockedNumbers(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_BLOCKED, emptySet()).orEmpty()

    fun addBlocked(context: Context, number: String) {
        val set = blockedNumbers(context).toMutableSet().apply { add(number.normalizeNumber()) }
        prefs(context).edit().putStringSet(KEY_BLOCKED, set).apply()
    }

    fun removeBlocked(context: Context, number: String) {
        val set = blockedNumbers(context).toMutableSet().apply { remove(number.normalizeNumber()) }
        prefs(context).edit().putStringSet(KEY_BLOCKED, set).apply()
    }

    fun blockAnonymous(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BLOCK_ANON, false)

    fun setBlockAnonymous(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean(KEY_BLOCK_ANON, value).apply()

    fun blockNonContacts(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BLOCK_NON_CONTACTS, false)

    fun setBlockNonContacts(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean(KEY_BLOCK_NON_CONTACTS, value).apply()

    /** True if [number] is on the user's blocklist. */
    fun isBlocked(context: Context, number: String?): Boolean {
        val n = number?.normalizeNumber() ?: return false
        return n in blockedNumbers(context)
    }

    private fun String.normalizeNumber(): String = replace("\\s".toRegex(), "")
}
