package network.bisq.mobile.node.settings.backup.domain

import network.bisq.mobile.node.settings.backup.domain.BackupPasswordPolicy.Violation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BackupPasswordPolicyTest {
    @Test
    fun `when the password has 11 characters then it is too short`() {
        assertEquals(Violation.TOO_SHORT, BackupPasswordPolicy.findViolation("abcdefghijk"))
    }

    @Test
    fun `when the password has 12 different characters then it is accepted`() {
        assertNull(BackupPasswordPolicy.findViolation("abcdefghijkl"))
    }

    @Test
    fun `when the password is a passphrase with spaces then it is accepted`() {
        assertNull(BackupPasswordPolicy.findViolation("correct horse battery staple"))
    }

    @Test
    fun `when the password has 256 characters then it is accepted`() {
        assertNull(BackupPasswordPolicy.findViolation("abcde".repeat(51) + "f"))
    }

    @Test
    fun `when the password has 257 characters then it is too long`() {
        assertEquals(Violation.TOO_LONG, BackupPasswordPolicy.findViolation("abcde".repeat(51) + "fg"))
    }

    @Test
    fun `when the password repeats one character then it has too few distinct characters`() {
        assertEquals(Violation.TOO_FEW_DISTINCT_CHARACTERS, BackupPasswordPolicy.findViolation("aaaaaaaaaaaa"))
    }

    @Test
    fun `when the password has 4 distinct characters then it has too few distinct characters`() {
        assertEquals(Violation.TOO_FEW_DISTINCT_CHARACTERS, BackupPasswordPolicy.findViolation("abcdabcdabcd"))
    }

    @Test
    fun `when the password has exactly 5 distinct characters then it is accepted`() {
        assertNull(BackupPasswordPolicy.findViolation("abcdeabcdeab"))
    }

    @Test
    fun `when the password is only spaces then it is too short`() {
        assertEquals(Violation.TOO_SHORT, BackupPasswordPolicy.findViolation(" ".repeat(12)))
    }

    @Test
    fun `when surrounding spaces pad a short password then it is still too short`() {
        assertEquals(Violation.TOO_SHORT, BackupPasswordPolicy.findViolation("  abcdefghijk  "))
    }

    @Test
    fun `when the password has surrounding whitespace then normalizing drops only that`() {
        assertEquals("correct horse battery", BackupPasswordPolicy.normalize(" \tcorrect horse battery \n"))
    }

    @Test
    fun `when an accent is typed precomposed or combining then both normalize the same`() {
        val precomposed = "caf\u00e9 au lait"
        val combining = "cafe\u0301 au lait"

        assertEquals(BackupPasswordPolicy.normalize(precomposed), BackupPasswordPolicy.normalize(combining))
    }

    @Test
    fun `when a CJK keyboard types full-width letters then they normalize to plain ones`() {
        assertEquals("password", BackupPasswordPolicy.normalize("\uff50\uff41\uff53\uff53\uff57\uff4f\uff52\uff44"))
    }

    @Test
    fun `when a no-break space surrounds the password then it is dropped like a space`() {
        assertEquals("correct horse battery", BackupPasswordPolicy.normalize("\u00a0correct horse battery\u00a0"))
    }

    @Test
    fun `when a combining accent is typed then the length counts it once`() {
        // 11 visible characters, 12 code points before normalization.
        assertEquals(Violation.TOO_SHORT, BackupPasswordPolicy.findViolation("abcdefghije\u0301"))
    }

    @Test
    fun `when characters are outside the basic plane then length counts code points`() {
        // Each emoji is two UTF-16 units, so 6 of them are 12 units but only 6 characters.
        assertEquals(Violation.TOO_SHORT, BackupPasswordPolicy.findViolation("😀😁😂😃😄😅"))
        assertNull(BackupPasswordPolicy.findViolation("😀😁😂😃😄😅😆😇😈😉😊😋"))
    }
}
