package it.iotatec.callhub.util

/** Semantic-version comparison (MAJOR.MINOR.PATCH), ignoring any -suffix. */
object SemVer {

    /** True if [candidate] is a newer version than [current]. */
    fun isNewer(candidate: String, current: String): Boolean {
        val a = parts(candidate)
        val b = parts(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    private fun parts(v: String): List<Int> =
        v.substringBefore('-').split('.').map { it.trim().toIntOrNull() ?: 0 }
}
