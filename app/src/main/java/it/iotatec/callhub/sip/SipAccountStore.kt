package it.iotatec.callhub.sip

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Persists a single SIP account. The password (and all fields) are stored in
 * [EncryptedSharedPreferences], backed by a Keystore master key.
 */
object SipAccountStore {

    private const val PREFS = "sip_account_secure"

    private fun prefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context.applicationContext,
            PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun save(context: Context, account: SipAccount) {
        runCatching {
            prefs(context).edit()
                .putString("displayName", account.displayName)
                .putString("username", account.username)
                .putString("domain", account.domain)
                .putString("password", account.password)
                .putInt("port", account.port)
                .putString("transport", account.transport.name)
                .apply()
        }
    }

    fun load(context: Context): SipAccount? = runCatching {
        val p = prefs(context)
        val username = p.getString("username", null) ?: return null
        val domain = p.getString("domain", null) ?: return null
        SipAccount(
            displayName = p.getString("displayName", username).orEmpty(),
            username = username,
            domain = domain,
            password = p.getString("password", "").orEmpty(),
            port = p.getInt("port", 5060),
            transport = runCatching {
                SipAccount.Transport.valueOf(p.getString("transport", "UDP").orEmpty())
            }.getOrDefault(SipAccount.Transport.UDP)
        )
    }.getOrNull()

    fun clear(context: Context) {
        runCatching { prefs(context).edit().clear().apply() }
    }
}
