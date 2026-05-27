package network.bisq.mobile.client.settings.payment_accounts_musig.model

import network.bisq.mobile.client.common.presentation.model.account.FiatPaymentMethodChargebackRiskVO
import network.bisq.mobile.client.common.presentation.model.account.PaymentTypeVO
import network.bisq.mobile.client.payment_accounts.presentation.payment_accounts_list.model.toVO
import network.bisq.mobile.domain.model.account.PaymentAccountPayload
import network.bisq.mobile.domain.model.account.fiat.Country
import network.bisq.mobile.domain.model.account.fiat.FiatCurrency
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentAccount
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk
import network.bisq.mobile.domain.model.account.fiat.UserDefinedFiatAccount
import network.bisq.mobile.domain.model.account.fiat.UserDefinedFiatAccountPayload
import network.bisq.mobile.domain.model.account.fiat.ZelleAccount
import network.bisq.mobile.domain.model.account.fiat.ZelleAccountPayload
import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FiatAccountVOTest {
    @Test
    fun `when mapping ZelleAccount then maps to FiatAccountVO with ZELLE payment method`() {
        // Given
        val account = sampleZelleAccount()

        // When
        val result = account.toVO()

        // Then
        assertNotNull(result)
        assertEquals("Zelle Main", result.accountName)
        assertEquals(FiatPaymentMethodChargebackRiskVO.LOW, result.chargebackRisk)
        assertEquals(PaymentTypeVO.ZELLE, result.paymentType)
        assertEquals("Zelle", result.paymentMethodName)
        assertEquals("United States", result.country)
        assertEquals("USD", result.currency)
    }

    @Test
    fun `when mapping UserDefinedFiatAccount then maps to FiatAccountVO with CUSTOM payment method`() {
        // Given
        val account = sampleUserDefinedFiatAccount()

        // When
        val result = account.toVO()

        // Then
        assertNotNull(result)
        assertEquals("Custom Main", result.accountName)
        assertEquals(FiatPaymentMethodChargebackRiskVO.VERY_LOW, result.chargebackRisk)
        assertEquals(PaymentTypeVO.CUSTOM, result.paymentType)
        assertEquals("My Custom Method", result.paymentMethodName)
        assertEquals(EMPTY_STRING, result.country)
        assertEquals(EMPTY_STRING, result.currency)
    }

    @Test
    fun `when mapping account with null chargeback risk then maps remaining payload fields`() {
        // Given
        val account = sampleZelleAccountWithNullOptionalFields()

        // When
        val result = account.toVO()

        // Then
        assertNotNull(result)
        assertNull(result.chargebackRisk)
        assertEquals("Zelle", result.paymentMethodName)
        assertEquals("United States", result.country)
        assertEquals("USD", result.currency)
    }

    @Test
    fun `when mapping unsupported FiatPaymentAccount implementation then returns null`() {
        // Given
        val account = UnsupportedFiatPaymentAccount()

        // When
        val result = account.toVO()

        // Then
        assertNull(result)
    }

    private fun sampleZelleAccount(): ZelleAccount =
        ZelleAccount(
            accountName = "Zelle Main",
            accountPayload =
                ZelleAccountPayload(
                    holderName = "Alice Doe",
                    emailOrMobileNr = "alice@example.com",
                    chargebackRisk = FiatPaymentMethodChargebackRisk.LOW,
                    paymentMethodName = "Zelle",
                    currency = FiatCurrency(code = "USD", name = "US Dollar"),
                    country = Country(code = "US", name = "United States"),
                ),
            creationDate = null,
            tradeLimitInfo = null,
            tradeDuration = null,
        )

    private fun sampleZelleAccountWithNullOptionalFields(): ZelleAccount =
        ZelleAccount(
            accountName = "Zelle Null Fields",
            accountPayload =
                ZelleAccountPayload(
                    holderName = "Alice Doe",
                    emailOrMobileNr = "alice@example.com",
                    chargebackRisk = null,
                    country = Country(code = "US", name = "United States"),
                    paymentMethodName = "Zelle",
                    currency = FiatCurrency(code = "USD", name = "US Dollar"),
                ),
            creationDate = null,
            tradeLimitInfo = null,
            tradeDuration = null,
        )

    private fun sampleUserDefinedFiatAccount(): UserDefinedFiatAccount =
        UserDefinedFiatAccount(
            accountName = "Custom Main",
            accountPayload =
                UserDefinedFiatAccountPayload(
                    accountData = "iban:DE89370400440532013000",
                    chargebackRisk = FiatPaymentMethodChargebackRisk.VERY_LOW,
                    paymentMethodName = "My Custom Method",
                ),
            creationDate = null,
            tradeLimitInfo = null,
            tradeDuration = null,
        )

    private class UnsupportedFiatPaymentAccount : FiatPaymentAccount {
        override val accountName: String = "Unsupported"
        override val accountPayload: PaymentAccountPayload = object : PaymentAccountPayload {}
        override val creationDate: String? = null
        override val tradeLimitInfo: String? = null
        override val tradeDuration: String? = null
    }
}
