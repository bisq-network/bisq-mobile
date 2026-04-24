package network.bisq.mobile.data.replicated.common.validation

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PaymentAccountValidationTest {
    @Test
    fun `validateHolderName accepts trimmed boundary lengths`() {
        PaymentAccountValidation.validateHolderName("  ${"a".repeat(PaymentAccountValidation.HOLDER_NAME_MIN_LENGTH)}  ")
        PaymentAccountValidation.validateHolderName("  ${"a".repeat(PaymentAccountValidation.HOLDER_NAME_MAX_LENGTH)}  ")
    }

    @Test
    fun `validateHolderName rejects below min length`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                PaymentAccountValidation.validateHolderName("a")
            }
        assertTrue(exception.message?.contains("Holder name must be between") == true)
    }

    @Test
    fun `validateHolderName rejects above max length`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                PaymentAccountValidation.validateHolderName("a".repeat(PaymentAccountValidation.HOLDER_NAME_MAX_LENGTH + 1))
            }
        assertTrue(exception.message?.contains("Holder name must be between") == true)
    }

    @Test
    fun `validateHolderName rejects blank input`() {
        assertFailsWith<IllegalArgumentException> {
            PaymentAccountValidation.validateHolderName("   ")
        }
    }

    @Test
    fun `validateUniqueAccountName accepts trimmed boundary lengths`() {
        PaymentAccountValidation.validateUniqueAccountName("  ${"a".repeat(PaymentAccountValidation.ACCOUNT_NAME_MIN_LENGTH)}  ")
        PaymentAccountValidation.validateUniqueAccountName("  ${"a".repeat(PaymentAccountValidation.ACCOUNT_NAME_MAX_LENGTH)}  ")
    }

    @Test
    fun `validateUniqueAccountName rejects below min length`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                PaymentAccountValidation.validateUniqueAccountName("a")
            }
        assertTrue(exception.message?.contains("Unique account name must be between") == true)
    }

    @Test
    fun `validateUniqueAccountName rejects above max length`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                PaymentAccountValidation.validateUniqueAccountName("a".repeat(PaymentAccountValidation.ACCOUNT_NAME_MAX_LENGTH + 1))
            }
        assertTrue(exception.message?.contains("Unique account name must be between") == true)
    }

    @Test
    fun `validateUniqueAccountName rejects blank input`() {
        assertFailsWith<IllegalArgumentException> {
            PaymentAccountValidation.validateUniqueAccountName("   ")
        }
    }

    @Test
    fun `validateCurrencyCode accepts known fiat codes`() {
        PaymentAccountValidation.validateCurrencyCode("USD")
        PaymentAccountValidation.validateCurrencyCode("EUR")
    }

    @Test
    fun `validateCurrencyCode rejects unknown fiat code`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                PaymentAccountValidation.validateCurrencyCode("ZZZ")
            }
        assertTrue(exception.message?.contains("No Fiat currency found for ZZZ") == true)
    }

    @Test
    fun `validateCurrencyCodes accepts non-empty valid list`() {
        PaymentAccountValidation.validateCurrencyCodes(listOf("USD", "EUR"))
    }

    @Test
    fun `validateCurrencyCodes rejects empty list`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                PaymentAccountValidation.validateCurrencyCodes(emptyList())
            }
        assertTrue(exception.message?.contains("must not be empty") == true)
    }

    @Test
    fun `validateCurrencyCodes rejects list containing invalid code`() {
        assertFailsWith<IllegalArgumentException> {
            PaymentAccountValidation.validateCurrencyCodes(listOf("USD", "ZZZ"))
        }
    }

    @Test
    fun `validateCurrencyCodes with allowed list accepts valid allowed currencies`() {
        PaymentAccountValidation.validateCurrencyCodes(
            currencyCodes = listOf("USD"),
            allowedCurrencyCodes = listOf("USD", "EUR"),
            contextDescription = "test context",
        )
    }

    @Test
    fun `validateCurrencyCodes with allowed list rejects empty requested list`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                PaymentAccountValidation.validateCurrencyCodes(
                    currencyCodes = emptyList(),
                    allowedCurrencyCodes = listOf("USD", "EUR"),
                    contextDescription = "test context",
                )
            }
        assertTrue(exception.message?.contains("Currency codes list must not be empty") == true)
    }

    @Test
    fun `validateCurrencyCodes with allowed list rejects empty allowed list`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                PaymentAccountValidation.validateCurrencyCodes(
                    currencyCodes = listOf("USD"),
                    allowedCurrencyCodes = emptyList(),
                    contextDescription = "test context",
                )
            }
        assertTrue(exception.message?.contains("Allowed currency codes list must not be empty") == true)
    }

    @Test
    fun `validateCurrencyCodes with allowed list rejects disallowed currency`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                PaymentAccountValidation.validateCurrencyCodes(
                    currencyCodes = listOf("EUR"),
                    allowedCurrencyCodes = listOf("USD"),
                    contextDescription = "test context",
                )
            }
        assertTrue(exception.message?.contains("is not supported for test context") == true)
    }

    @Test
    fun `validateCurrencyCodes with allowed list rejects unknown requested currency`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                PaymentAccountValidation.validateCurrencyCodes(
                    currencyCodes = listOf("ZZZ"),
                    allowedCurrencyCodes = listOf("USD", "EUR"),
                    contextDescription = "test context",
                )
            }
        assertTrue(exception.message?.contains("No Fiat currency found for ZZZ") == true)
    }

    @Test
    fun `validateFasterPaymentsSortCode accepts exactly six digits`() {
        PaymentAccountValidation.validateFasterPaymentsSortCode("123456")
    }

    @Test
    fun `validateFasterPaymentsSortCode rejects wrong length`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                PaymentAccountValidation.validateFasterPaymentsSortCode("12345")
            }
        assertTrue(exception.message?.contains("6 numbers") == true)
    }

    @Test
    fun `validateFasterPaymentsSortCode rejects non digit characters`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                PaymentAccountValidation.validateFasterPaymentsSortCode("12A456")
            }
        assertTrue(exception.message?.contains("must consist of numbers") == true)
    }

    @Test
    fun `validateFasterPaymentsAccountNr accepts exactly eight digits`() {
        PaymentAccountValidation.validateFasterPaymentsAccountNr("12345678")
    }

    @Test
    fun `validateFasterPaymentsAccountNr rejects wrong length`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                PaymentAccountValidation.validateFasterPaymentsAccountNr("1234567")
            }
        assertTrue(exception.message?.contains("8 numbers") == true)
    }

    @Test
    fun `validateFasterPaymentsAccountNr rejects non digit characters`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                PaymentAccountValidation.validateFasterPaymentsAccountNr("12345A78")
            }
        assertTrue(exception.message?.contains("must consist of numbers") == true)
    }
}
