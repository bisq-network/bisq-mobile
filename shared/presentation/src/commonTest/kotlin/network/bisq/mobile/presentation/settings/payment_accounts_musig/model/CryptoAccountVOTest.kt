package network.bisq.mobile.presentation.settings.payment_accounts_musig.model

import network.bisq.mobile.domain.model.account.crypto.MoneroAccount
import network.bisq.mobile.domain.model.account.crypto.MoneroAccountPayload
import network.bisq.mobile.domain.model.account.crypto.OtherCryptoAssetAccount
import network.bisq.mobile.domain.model.account.crypto.OtherCryptoAssetAccountPayload
import network.bisq.mobile.presentation.common.model.account.PaymentTypeVO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CryptoAccountVOTest {
    @Test
    fun `when mapping MoneroAccount then maps to CryptoAccountVO with XMR payment method`() {
        // Given
        val account = sampleMoneroAccount()

        // When
        val result = account.toVO()

        // Then
        assertNotNull(result)
        assertEquals("Monero Main", result.accountName)
        assertEquals("Monero (XMR)", result.currencyName)
        assertEquals("84ABcdXy12pqRstUvw3456EfGh7890JKLMnOPQ", result.address)
        assertEquals(PaymentTypeVO.XMR, result.paymentType)
    }

    @Test
    fun `when mapping OtherCryptoAssetAccount with ETH then maps to ETH payment method`() {
        // Given
        val account = sampleOtherCryptoAssetAccount(currencyCode = "ETH", currencyName = "Ethereum")

        // When
        val result = account.toVO()

        // Then
        assertNotNull(result)
        assertEquals("Other Crypto", result.accountName)
        assertEquals("Ethereum (ETH)", result.currencyName)
        assertEquals("0x1fA2b3C4d5E6f708901234567890AbCdEf123456", result.address)
        assertEquals(PaymentTypeVO.ETH, result.paymentType)
    }

    @Test
    fun `when mapping OtherCryptoAssetAccount with LTC then maps to LTC payment method`() {
        // Given
        val account = sampleOtherCryptoAssetAccount(currencyCode = "LTC", currencyName = "Litecoin")

        // When
        val result = account.toVO()

        // Then
        assertNotNull(result)
        assertEquals(PaymentTypeVO.LTC, result.paymentType)
    }

    @Test
    fun `when mapping OtherCryptoAssetAccount with BSQ then maps to BSQ payment method`() {
        // Given
        val account = sampleOtherCryptoAssetAccount(currencyCode = "BSQ", currencyName = "Bisq DAO")

        // When
        val result = account.toVO()

        // Then
        assertNotNull(result)
        assertEquals(PaymentTypeVO.BSQ, result.paymentType)
    }

    @Test
    fun `when mapping OtherCryptoAssetAccount with ETC then maps to ETC payment method`() {
        // Given
        val account = sampleOtherCryptoAssetAccount(currencyCode = "ETC", currencyName = "Ethereum Classic")

        // When
        val result = account.toVO()

        // Then
        assertNotNull(result)
        assertEquals(PaymentTypeVO.ETC, result.paymentType)
    }

    @Test
    fun `when mapping OtherCryptoAssetAccount with L-BTC then maps to LBTC payment method`() {
        // Given
        val account = sampleOtherCryptoAssetAccount(currencyCode = "L-BTC", currencyName = "Liquid Bitcoin")

        // When
        val result = account.toVO()

        // Then
        assertNotNull(result)
        assertEquals(PaymentTypeVO.LBTC, result.paymentType)
    }

    @Test
    fun `when mapping OtherCryptoAssetAccount with LN-BTC then maps to LNBTC payment method`() {
        // Given
        val account = sampleOtherCryptoAssetAccount(currencyCode = "LN-BTC", currencyName = "Lightning Bitcoin")

        // When
        val result = account.toVO()

        // Then
        assertNotNull(result)
        assertEquals(PaymentTypeVO.LNBTC, result.paymentType)
    }

    @Test
    fun `when mapping OtherCryptoAssetAccount with GRIN then maps to GRIN payment method`() {
        // Given
        val account = sampleOtherCryptoAssetAccount(currencyCode = "GRIN", currencyName = "Grin")

        // When
        val result = account.toVO()

        // Then
        assertNotNull(result)
        assertEquals(PaymentTypeVO.GRIN, result.paymentType)
    }

    @Test
    fun `when mapping OtherCryptoAssetAccount with ZEC then maps to ZEC payment method`() {
        // Given
        val account = sampleOtherCryptoAssetAccount(currencyCode = "ZEC", currencyName = "Zcash")

        // When
        val result = account.toVO()

        // Then
        assertNotNull(result)
        assertEquals(PaymentTypeVO.ZEC, result.paymentType)
    }

    @Test
    fun `when mapping OtherCryptoAssetAccount with DOGE then maps to DOGE payment method`() {
        // Given
        val account = sampleOtherCryptoAssetAccount(currencyCode = "DOGE", currencyName = "Dogecoin")

        // When
        val result = account.toVO()

        // Then
        assertNotNull(result)
        assertEquals(PaymentTypeVO.DOGE, result.paymentType)
    }

    @Test
    fun `when mapping OtherCryptoAssetAccount with unsupported currency code then returns null`() {
        // Given
        val account = sampleOtherCryptoAssetAccount(currencyCode = "ABC", currencyName = "Unknown")

        // When
        val result = account.toVO()

        // Then
        assertNull(result)
    }

    private fun sampleMoneroAccount(): MoneroAccount =
        MoneroAccount(
            accountName = "Monero Main",
            accountPayload =
                MoneroAccountPayload(
                    currencyName = "Monero",
                    address = "84ABcdXy12pqRstUvw3456EfGh7890JKLMnOPQ",
                    isInstant = false,
                    useSubAddresses = false,
                    supportAutoConf = true,
                    currencyCode = "XMR",
                ),
            creationDate = null,
            tradeLimitInfo = null,
            tradeDuration = null,
        )

    private fun sampleOtherCryptoAssetAccount(
        currencyCode: String,
        currencyName: String,
    ): OtherCryptoAssetAccount =
        OtherCryptoAssetAccount(
            accountName = "Other Crypto",
            accountPayload =
                OtherCryptoAssetAccountPayload(
                    currencyCode = currencyCode,
                    currencyName = currencyName,
                    address = "0x1fA2b3C4d5E6f708901234567890AbCdEf123456",
                    isInstant = false,
                    supportAutoConf = false,
                ),
            creationDate = null,
            tradeLimitInfo = null,
            tradeDuration = null,
        )
}
