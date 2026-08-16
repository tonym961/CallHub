package it.iotatec.callhub.data.repo

import android.content.Context
import it.iotatec.callhub.dialer.spam.SpamRepository
import it.iotatec.callhub.dialer.spam.SystemBlocklist
import it.iotatec.callhub.sip.SipAccount
import it.iotatec.callhub.sip.SipAccountStore
import it.iotatec.callhub.ui.AppTheme
import org.json.JSONArray
import org.json.JSONObject

/** Exports/imports all app settings as JSON, and imports a plain-text blocklist. */
object BackupManager {

    fun exportJson(context: Context): String {
        val root = JSONObject()

        val spam = context.getSharedPreferences("spam_policy", Context.MODE_PRIVATE)
        root.put("blocked", JSONArray(spam.getStringSet("blocked_numbers", emptySet())))
        root.put("block_anonymous", spam.getBoolean("block_anonymous", false))
        root.put("block_non_contacts", spam.getBoolean("block_non_contacts", false))

        val fav = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
        root.put("favorites", JSONArray(fav.getStringSet("numbers", emptySet())))

        context.getSharedPreferences("quick_replies", Context.MODE_PRIVATE)
            .getString("replies", null)?.let { root.put("quick_replies", it) }

        val sipAccounts = SipAccountStore.loadAll(context)
        if (sipAccounts.isNotEmpty()) {
            root.put("sipAccounts", JSONArray().apply {
                sipAccounts.forEach { acc ->
                    put(JSONObject().apply {
                        put("displayName", acc.displayName)
                        put("username", acc.username)
                        put("domain", acc.domain)
                        put("password", acc.password)
                        put("port", acc.port)
                        put("transport", acc.transport.name)
                    })
                }
            })
        }

        root.put("theme", context.getSharedPreferences("theme", Context.MODE_PRIVATE).getString("mode", "SYSTEM"))
        return root.toString(2)
    }

    fun importJson(context: Context, json: String) {
        val root = JSONObject(json)

        context.getSharedPreferences("spam_policy", Context.MODE_PRIVATE).edit().apply {
            root.optJSONArray("blocked")?.let { putStringSet("blocked_numbers", it.toStringSet()) }
            putBoolean("block_anonymous", root.optBoolean("block_anonymous", false))
            putBoolean("block_non_contacts", root.optBoolean("block_non_contacts", false))
            apply()
        }

        root.optJSONArray("favorites")?.let {
            context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
                .edit().putStringSet("numbers", it.toStringSet()).apply()
        }

        val replies = root.optString("quick_replies", "")
        if (replies.isNotBlank()) {
            context.getSharedPreferences("quick_replies", Context.MODE_PRIVATE)
                .edit().putString("replies", replies).apply()
        }

        fun sipFrom(s: JSONObject) = SipAccount(
            displayName = s.optString("displayName"),
            username = s.optString("username"),
            domain = s.optString("domain"),
            password = s.optString("password"),
            port = s.optInt("port", 5060),
            transport = runCatching { SipAccount.Transport.valueOf(s.optString("transport", "UDP")) }
                .getOrDefault(SipAccount.Transport.UDP)
        )
        root.optJSONArray("sipAccounts")?.let { arr ->
            SipAccountStore.saveAll(context, (0 until arr.length()).map { sipFrom(arr.getJSONObject(it)) })
        }
        // Backward compatibility with the old single-account backup format.
        root.optJSONObject("sip")?.let { SipAccountStore.addOrUpdate(context, sipFrom(it)) }

        context.getSharedPreferences("theme", Context.MODE_PRIVATE)
            .edit().putString("mode", root.optString("theme", "SYSTEM")).apply()
        AppTheme.load(context)
    }

    /** Import a newline-separated list of numbers into the local + system blocklist. */
    fun importBlocklist(context: Context, text: String): Int {
        var count = 0
        text.lineSequence().map { it.trim() }.filter { it.isNotBlank() && it.first().let { c -> c.isDigit() || c == '+' } }
            .forEach {
                SpamRepository.addBlocked(context, it)
                SystemBlocklist.block(context, it)
                count++
            }
        return count
    }

    private fun JSONArray.toStringSet(): Set<String> =
        (0 until length()).map { getString(it) }.toSet()
}
