package network.bisq.mobile.data.replicated.common.validation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SepaPaymentAccountValidationTest {
    @Test
    fun `test valid ibans should pass`() {
        val validIbans =
            listOf(
                "DE89370400440532013000", // Germany
                "FR1420041010050500013M02606", // France
                "GB29NWBK60161331926819", // UK
                "GR1601101250000000012300695", // Greece
                "ES9121000418450200051332", // Spain
            )

        validIbans.forEach { iban ->
            SepaPaymentAccountValidation.validateIban(iban)
        }
    }

    @Test
    fun `test iban with invalid checksum should fail`() {
        val invalidChecksumIban = "DE89370400440532013001"
        val exception =
            assertFailsWith<IllegalArgumentException> {
                SepaPaymentAccountValidation.validateIban(invalidChecksumIban)
            }
        assertEquals(true, exception.message?.contains("checksum"))
    }

    @Test
    fun `test iban with invalid format should fail`() {
        val invalidIban = "D189370400440532013000"
        val exception =
            assertFailsWith<IllegalArgumentException> {
                SepaPaymentAccountValidation.validateIban(invalidIban)
            }
        assertEquals(true, exception.message?.contains("Invalid IBAN format"))
    }

    @Test
    fun `test iban with wrong length should fail`() {
        val shortIban = "DE89370400"
        val exception =
            assertFailsWith<IllegalArgumentException> {
                SepaPaymentAccountValidation.validateIban(shortIban)
            }
        assertEquals(true, exception.message?.contains("length"))
    }

    @Test
    fun `test null or empty iban should fail`() {
        val nullException =
            assertFailsWith<IllegalArgumentException> {
                SepaPaymentAccountValidation.validateIban(null)
            }
        assertEquals(true, nullException.message?.contains("must not be null or empty"))

        val emptyException =
            assertFailsWith<IllegalArgumentException> {
                SepaPaymentAccountValidation.validateIban("")
            }
        assertEquals(true, emptyException.message?.contains("must not be null or empty"))
    }

    @Test
    fun `test iban with special characters should fail`() {
        val invalidIban = "DE89-3704-0044-0532-0130-00"
        val exception =
            assertFailsWith<IllegalArgumentException> {
                SepaPaymentAccountValidation.validateIban(invalidIban)
            }
        assertEquals(true, exception.message?.contains("Invalid IBAN format"))
    }

    @Test
    fun `test iban with spaces should fail like backend validator`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                SepaPaymentAccountValidation.validateIban("DE89 3704 0044 0532 0130 00")
            }
        assertEquals(true, exception.message?.contains("Invalid IBAN format"))
    }

    @Test
    fun `test valid sepa iban should pass`() {
        SepaPaymentAccountValidation.validateSepaIban(
            iban = "DE89370400440532013000",
            sepaCountryCodes = setOf("DE", "FR", "ES", "IT"),
        )
    }

    @Test
    fun `test valid sepa iban with spaces and lowercase should pass`() {
        SepaPaymentAccountValidation.validateSepaIban(
            iban = "de89 3704 0044 0532 0130 00",
            sepaCountryCodes = setOf("DE", "FR", "ES", "IT"),
        )
    }

    @Test
    fun `test non sepa iban country should fail`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                SepaPaymentAccountValidation.validateSepaIban(
                    iban = "GB29NWBK60161331926819",
                    sepaCountryCodes = setOf("DE", "FR", "ES", "IT"),
                )
            }
        assertEquals(true, exception.message?.contains("is not a SEPA member country"))
    }

    @Test
    fun `test valid bic should pass`() {
        SepaPaymentAccountValidation.validateBic("DEUTDEFF")
        SepaPaymentAccountValidation.validateBic("DEUTDEFF500")
    }

    @Test
    fun `test bic with invalid length should fail`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                SepaPaymentAccountValidation.validateBic("DEUTDE")
            }
        assertEquals(true, exception.message?.contains("length"))
    }

    @Test
    fun `test bic with invalid format should fail`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                SepaPaymentAccountValidation.validateBic("DEUT12FF")
            }
        assertEquals(true, exception.message?.contains("Invalid BIC/SWIFT format"))
    }

    @Test
    fun `test bic location code starting with reserved digit should fail`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                SepaPaymentAccountValidation.validateBic("DEUTDE0F")
            }
        assertEquals(true, exception.message?.contains("cannot start with 0 or 1"))
    }

    @Test
    fun `test bic location code ending with O should fail`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                SepaPaymentAccountValidation.validateBic("DEUTDEFO")
            }
        assertEquals(true, exception.message?.contains("cannot end with letter O"))
    }

    @Test
    fun `test bic branch code starting with x must be xxx`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                SepaPaymentAccountValidation.validateBic("DEUTDEFFX01")
            }
        assertEquals(true, exception.message?.contains("starting with X must be XXX"))
    }

    @Test
    fun `test revolut bic should fail`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                SepaPaymentAccountValidation.validateBic("REVOLT21")
            }
        assertEquals(true, exception.message?.contains("Revolut"))
    }

    @Test
    fun `test iban country code match should pass`() {
        SepaPaymentAccountValidation.validateIbanMatchesCountryCode(
            iban = "DE89370400440532013000",
            countryCode = "DE",
        )
    }

    @Test
    fun `test iban country code mismatch should fail`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                SepaPaymentAccountValidation.validateIbanMatchesCountryCode(
                    iban = "DE89370400440532013000",
                    countryCode = "FR",
                )
            }
        assertEquals(true, exception.message?.contains("does not match declared country"))
    }

    @Test
    fun `test iban too short for country extraction should fail`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                SepaPaymentAccountValidation.validateIbanMatchesCountryCode(
                    iban = "D",
                    countryCode = "DE",
                )
            }
        assertEquals(true, exception.message?.contains("too short"))
    }
}
