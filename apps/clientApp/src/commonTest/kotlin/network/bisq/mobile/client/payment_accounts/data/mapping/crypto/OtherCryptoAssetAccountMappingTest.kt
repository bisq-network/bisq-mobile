package network.bisq.mobile.client.payment_accounts.data.mapping.crypto

import network.bisq.mobile.client.payment_accounts.data.model.crypto.CryptoPaymentRailDto
import network.bisq.mobile.client.payment_accounts.data.model.crypto.other_crypto.CreateOtherCryptoAssetAccountDto
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.other_crypto.CreateOtherCryptoAssetAccount
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.other_crypto.CreateOtherCryptoAssetAccountPayload
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class OtherCryptoAssetAccountMappingTest {
    @Test
    fun `toDto maps create OtherCryptoAssetAccount fields correctly`() {
        val domain =
            CreateOtherCryptoAssetAccount(
                accountName = "ETH Main",
                accountPayload =
                    CreateOtherCryptoAssetAccountPayload(
                        currencyCode = "ETH",
                        address = "eth-address",
                        isInstant = false,
                    ),
            )

        val dto = domain.toDto()

        assertIs<CreateOtherCryptoAssetAccountDto>(dto)
        assertEquals(CryptoPaymentRailDto.OTHER_CRYPTO_ASSET, dto.paymentRail)
        assertEquals("ETH Main", dto.accountName)
        assertEquals("ETH", dto.accountPayload.currencyCode)
        assertEquals("eth-address", dto.accountPayload.address)
        assertEquals(false, dto.accountPayload.isInstant)
    }
}
