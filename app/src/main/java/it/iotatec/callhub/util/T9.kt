package it.iotatec.callhub.util

/** Keypad (T9) matching: match a contact by name-to-digits or by number substring. */
object T9 {

    private fun toT9(text: String): String = text.lowercase().map { c ->
        when (c) {
            in 'a'..'c' -> '2'
            in 'd'..'f' -> '3'
            in 'g'..'i' -> '4'
            in 'j'..'l' -> '5'
            in 'm'..'o' -> '6'
            in 'p'..'s' -> '7'
            in 't'..'v' -> '8'
            in 'w'..'z' -> '9'
            else -> ' '
        }
    }.joinToString("")

    /** True if the typed [digits] match [name] (T9, per word) or [number] (substring). */
    fun matches(name: String, number: String, digits: String): Boolean {
        if (digits.isEmpty()) return false
        if (number.filter { it.isDigit() }.contains(digits)) return true
        return toT9(name).split(' ').any { it.startsWith(digits) }
    }
}
