package it.iotatec.callhub.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class T9Test {

    @Test fun matchesByNameStart() =
        assertTrue(T9.matches("Marco Bianchi", "+390612345678", "627")) // M-a-r

    @Test fun matchesBySecondWord() =
        assertTrue(T9.matches("Marco Bianchi", "+390612345678", "242")) // B-i-a -> 2-4-2

    @Test fun matchesByNumberSubstring() =
        assertTrue(T9.matches("Marco Bianchi", "+390612345678", "0612"))

    @Test fun noMatch() =
        assertFalse(T9.matches("Marco Bianchi", "+390612345678", "999"))

    @Test fun emptyDigitsNoMatch() =
        assertFalse(T9.matches("Marco Bianchi", "+390612345678", ""))
}
