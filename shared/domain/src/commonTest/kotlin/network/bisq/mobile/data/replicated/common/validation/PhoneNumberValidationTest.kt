package network.bisq.mobile.data.replicated.common.validation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PhoneNumberValidationTest {
    @Test
    fun `test valid german numbers`() {
        assertTrue(PhoneNumberValidation.isValid("+4915123456789", "DE"))
        assertTrue(PhoneNumberValidation.isValid("0151 23456789", "DE"))
        assertTrue(PhoneNumberValidation.isValid("004915123456789", "DE"))
    }

    @Test
    fun `test invalid german numbers`() {
        assertFalse(PhoneNumberValidation.isValid("12345", "DE"))
        assertFalse(PhoneNumberValidation.isValid("+49 15123", "DE"))
        assertFalse(PhoneNumberValidation.isValid("++4915123456789", "DE"))
        assertFalse(PhoneNumberValidation.isValid("+++4915123456789", "DE"))
    }

    @Test
    fun `test formatted number with valid digits should be accepted`() {
        assertTrue(PhoneNumberValidation.isValid("+49 151 2345 6789", "DE"))
    }

    @Test
    fun `test number with more than 15 digits is rejected`() {
        assertFalse(PhoneNumberValidation.isValid("+1 202 555 0171 23456", "US"))
    }

    @Test
    fun `test valid us numbers`() {
        assertTrue(PhoneNumberValidation.isValid("+1 202-555-0171", "US"))
        assertTrue(PhoneNumberValidation.isValid("202-555-0171", "US"))
        assertTrue(PhoneNumberValidation.isValid("+1 415 9604264", "US"))
    }

    @Test
    fun `test invalid us numbers`() {
        assertFalse(PhoneNumberValidation.isValid("123", "US"))
        assertFalse(PhoneNumberValidation.isValid("999-999-9999", "US"))
        assertFalse(PhoneNumberValidation.isValid("\u202D+1 415 9604264\u202C", "US"))
        assertFalse(PhoneNumberValidation.isValid("+1202555017123456", "US"))
    }

    @Test
    fun `test null or empty input`() {
        assertFalse(PhoneNumberValidation.isValid(null, "US"))
        assertFalse(PhoneNumberValidation.isValid("", "US"))
    }

    @Test
    fun `test invalid region`() {
        assertFalse(PhoneNumberValidation.isValid("+4915123456789", "XX"))
    }
}
