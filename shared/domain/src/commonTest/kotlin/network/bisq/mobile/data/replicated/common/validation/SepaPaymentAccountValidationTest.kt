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
    fun `test iban with invalid checksum still passes format validation`() {
        val invalidChecksumIban = "DE89370400440532013001"
        SepaPaymentAccountValidation.validateIban(invalidChecksumIban)
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
    fun `test empty iban should fail`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                SepaPaymentAccountValidation.validateIban("")
            }
        assertEquals(true, exception.message?.contains("must not be empty"))
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
    fun `test valid sepa iban should pass`() {
        SepaPaymentAccountValidation.validateSepaIban(
            iban = "DE89370400440532013000",
            sepaCountryCodes = listOf("DE", "FR", "ES", "IT"),
        )
    }

    @Test
    fun `test non sepa iban country should fail`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                SepaPaymentAccountValidation.validateSepaIban(
                    iban = "GB29NWBK60161331926819",
                    sepaCountryCodes = listOf("DE", "FR", "ES", "IT"),
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
        assertEquals(true, exception.message?.contains("Invalid BIC format"))
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
            countryCode = "de",
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
