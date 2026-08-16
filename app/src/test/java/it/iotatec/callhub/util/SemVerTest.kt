package it.iotatec.callhub.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SemVerTest {

    @Test fun newerPatch() = assertTrue(SemVer.isNewer("1.0.1", "1.0.0"))
    @Test fun newerMinor() = assertTrue(SemVer.isNewer("1.1.0", "1.0.9"))
    @Test fun newerMajor() = assertTrue(SemVer.isNewer("2.0.0", "1.9.9"))
    @Test fun sameVersionNotNewer() = assertFalse(SemVer.isNewer("1.0.0", "1.0.0"))
    @Test fun olderNotNewer() = assertFalse(SemVer.isNewer("1.0.0", "1.0.1"))
    @Test fun ignoresSuffix() = assertFalse(SemVer.isNewer("1.0.0", "1.0.0-full"))
    @Test fun suffixCandidateNewer() = assertTrue(SemVer.isNewer("1.1.0-full", "1.0.0"))
    @Test fun shorterVersion() = assertTrue(SemVer.isNewer("1.1", "1.0.5"))
}
