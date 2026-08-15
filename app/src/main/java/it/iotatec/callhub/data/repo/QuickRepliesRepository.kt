package it.iotatec.callhub.data.repo

import android.content.Context
import it.iotatec.callhub.R

/**
 * User-editable reject-with-message quick replies. Falls back to the three
 * localized defaults until the user customizes them.
 */
object QuickRepliesRepository {

    private const val PREFS = "quick_replies"
    private const val KEY = "replies"
    private const val SEP = "\n"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun get(context: Context): List<String> {
        val saved = prefs(context).getString(KEY, null)
        if (saved != null) return saved.split(SEP).filter { it.isNotBlank() }
        return listOf(
            context.getString(R.string.quick_reply_1),
            context.getString(R.string.quick_reply_2),
            context.getString(R.string.quick_reply_3)
        )
    }

    fun set(context: Context, replies: List<String>) {
        prefs(context).edit()
            .putString(KEY, replies.filter { it.isNotBlank() }.joinToString(SEP))
            .apply()
    }

    fun add(context: Context, reply: String) {
        if (reply.isBlank()) return
        set(context, get(context) + reply.trim())
    }

    fun remove(context: Context, reply: String) {
        set(context, get(context) - reply)
    }
}
