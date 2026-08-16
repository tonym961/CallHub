package it.iotatec.callhub.dialer.spam

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * Free, real caller-ID: a community spam list downloaded from a user-configured
 * URL (one number per line), cached locally. Numbers on the list are flagged SPAM.
 */
object CommunityReputationStore {

    private const val PREFS = "reputation"
    private const val KEY_URL = "url"
    private const val KEY_SET = "numbers"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun norm(n: String) = n.replace("\\s".toRegex(), "")

    fun url(context: Context): String = prefs(context).getString(KEY_URL, "").orEmpty()
    fun setUrl(context: Context, url: String) = prefs(context).edit().putString(KEY_URL, url.trim()).apply()

    fun numbers(context: Context): Set<String> = prefs(context).getStringSet(KEY_SET, emptySet()).orEmpty()

    fun isSpam(context: Context, number: String): Boolean =
        number.isNotBlank() && norm(number) in numbers(context)

    /** Download + cache the list; returns how many numbers were loaded. */
    suspend fun refresh(context: Context): Int = withContext(Dispatchers.IO) {
        val u = url(context)
        if (u.isBlank()) return@withContext 0
        val text = runCatching { URL(u).readText() }.getOrNull() ?: return@withContext 0
        val set = text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() && it.first().let { c -> c.isDigit() || c == '+' } }
            .map { norm(it) }
            .toSet()
        prefs(context).edit().putStringSet(KEY_SET, set).apply()
        set.size
    }
}

/** Reputation provider backed by the community list. */
class CommunityReputationProvider(context: Context) : ReputationProvider {
    private val app = context.applicationContext
    override suspend fun lookup(number: String): ReputationProvider.Verdict =
        if (CommunityReputationStore.isSpam(app, number)) ReputationProvider.Verdict.SPAM
        else ReputationProvider.Verdict.UNKNOWN
}
