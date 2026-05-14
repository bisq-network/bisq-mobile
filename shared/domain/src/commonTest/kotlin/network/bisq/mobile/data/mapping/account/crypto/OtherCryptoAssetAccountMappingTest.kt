package network.bisq.mobile.data.mapping.account.crypto

import network.bisq.mobile.data.model.account.crypto.CryptoPaymentRailDto
import network.bisq.mobile.data.model.account.crypto.create.CreateOtherCryptoAssetAccountDto
import network.bisq.mobile.domain.model.account.create.crypto.CreateOtherCryptoAssetAccount
import network.bisq.mobile.domain.model.account.create.crypto.CreateOtherCryptoAssetAccountPayload
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
