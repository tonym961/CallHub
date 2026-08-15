package it.iotatec.callhub.sip

import android.content.Context

/**
 * Persists a single SIP account.
 *
 * SECURITY TODO: the password is stored in plain SharedPreferences here only to
 * keep the scaffold dependency-free. Before production, move it to
 * EncryptedSharedPreferences (androidx.security-crypto) or the Android Keystore.
 */
object SipAccountStore {

    private const val PREFS = "sip_account"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(context: Context, account: SipAccount) {
        prefs(context).edit()
            .putString("displayName", account.displayName)
            .putString("username", account.username)
            .putString("domain", account.domain)
            .putString("password", account.password)
            .putInt("port", account.port)
            .putString("transport", account.transport.name)
            .apply()
    }

    fun load(context: Context): SipAccount? {
        val p = prefs(context)
        val username = p.getString("username", null) ?: return null
        val domain = p.getString("domain", null) ?: return null
        return SipAccount(
            displayName = p.getString("displayName", username).orEmpty(),
            username = username,
            domain = domain,
            password = p.getString("password", "").orEmpty(),
            port = p.getInt("port", 5060),
            transport = runCatching {
                SipAccount.Transport.valueOf(p.getString("transport", "UDP").orEmpty())
            }.getOrDefault(SipAccount.Transport.UDP)
        )
    }

    fun clear(context: Context) = prefs(context).edit().clear().apply()
}
