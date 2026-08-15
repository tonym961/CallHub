package it.iotatec.callhub.data.repo

import android.content.Context

/** Favorite (speed-dial) numbers, persisted in SharedPreferences. */
object FavoritesRepository {

    private const val PREFS = "favorites"
    private const val KEY = "numbers"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun normalize(number: String) = number.replace("\\s".toRegex(), "")

    fun favorites(context: Context): Set<String> =
        prefs(context).getStringSet(KEY, emptySet()).orEmpty()

    fun isFavorite(context: Context, number: String): Boolean =
        normalize(number) in favorites(context)

    /** Toggle favorite state; returns the new state. */
    fun toggle(context: Context, number: String): Boolean {
        val set = favorites(context).toMutableSet()
        val n = normalize(number)
        val nowFavorite = if (set.contains(n)) { set.remove(n); false } else { set.add(n); true }
        prefs(context).edit().putStringSet(KEY, set).apply()
        return nowFavorite
    }
}
