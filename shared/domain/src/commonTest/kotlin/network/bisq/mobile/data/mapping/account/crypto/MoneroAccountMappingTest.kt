package network.bisq.mobile.data.mapping.account.crypto

import network.bisq.mobile.data.model.account.crypto.CryptoPaymentRailDto
import network.bisq.mobile.data.model.account.crypto.create.CreateMoneroAccountDto
import network.bisq.mobile.domain.model.account.create.crypto.CreateMoneroAccount
import network.bisq.mobile.domain.model.account.create.crypto.CreateMoneroAccountPayload
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MoneroAccountMappingTest {
    @Test
    fun `toDto maps create MoneroAccount fields correctly`() {
        val domain =
            CreateMoneroAccount(
                accountName = "XMR Main",
                accountPayload =
                    CreateMoneroAccountPayload(
                        address = "xmr-address",
                        isInstant = true,
                        isAutoConf = true,
                        autoConfNumConfirmations = 5,
                        autoConfMaxTradeAmount = 100_000L,
                        autoConfExplorerUrls = "https://explorer.example",
                        useSubAddresses = false,
                    ),
            )

        val dto = domain.toDto()

        assertIs<CreateMoneroAccountDto>(dto)
        assertEquals(CryptoPaymentRailDto.MONERO, dto.paymentRail)
        assertEquals("XMR Main", dto.accountName)
        assertEquals("xmr-address", dto.accountPayload.address)
        assertEquals(true, dto.accountPayload.isInstant)
        assertEquals(5, dto.accountPayload.autoConfNumConfirmations)
    }
}
