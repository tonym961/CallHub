package it.iotatec.callhub.sip

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists the list of SIP accounts, encrypted ([EncryptedSharedPreferences],
 * Keystore-backed). Accounts are keyed by [SipAccount.id] (username@domain).
 */
object SipAccountStore {

    private const val PREFS = "sip_account_secure"
    private const val KEY = "accounts"

    private fun prefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context.applicationContext, PREFS, masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun loadAll(context: Context): List<SipAccount> = runCatching {
        val raw = prefs(context).getString(KEY, null) ?: return emptyList()
        val arr = JSONArray(raw)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            SipAccount(
                displayName = o.optString("displayName"),
                username = o.optString("username"),
                domain = o.optString("domain"),
                password = o.optString("password"),
                port = o.optInt("port", 5060),
                transport = runCatching { SipAccount.Transport.valueOf(o.optString("transport", "UDP")) }
                    .getOrDefault(SipAccount.Transport.UDP)
            )
        }
    }.getOrDefault(emptyList())

    fun saveAll(context: Context, accounts: List<SipAccount>) {
        runCatching {
            val arr = JSONArray()
            accounts.forEach { a ->
                arr.put(JSONObject().apply {
                    put("displayName", a.displayName)
                    put("username", a.username)
                    put("domain", a.domain)
                    put("password", a.password)
                    put("port", a.port)
                    put("transport", a.transport.name)
                })
            }
            prefs(context).edit().putString(KEY, arr.toString()).apply()
        }
    }

    /** Add or replace an account (matched by id). */
    fun addOrUpdate(context: Context, account: SipAccount) {
        val list = loadAll(context).filterNot { it.id == account.id } + account
        saveAll(context, list)
    }

    fun remove(context: Context, id: String) {
        saveAll(context, loadAll(context).filterNot { it.id == id })
    }

    /** First account (backward-compatible convenience). */
    fun load(context: Context): SipAccount? = loadAll(context).firstOrNull()

    /** Legacy single-account save maps to add-or-update. */
    fun save(context: Context, account: SipAccount) = addOrUpdate(context, account)
}
