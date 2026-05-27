package network.bisq.mobile.client.payment_accounts.data.mapping.fiat

import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.FiatPaymentRailDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.user_defined.CreateUserDefinedFiatAccountDto
import network.bisq.mobile.domain.model.account.create.fiat.CreateUserDefinedFiatAccount
import network.bisq.mobile.domain.model.account.create.fiat.CreateUserDefinedFiatAccountPayload
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UserDefinedFiatAccountMappingTest {
    @Test
    fun `toDto maps create UserDefinedFiatAccount fields correctly`() {
        val domain =
            CreateUserDefinedFiatAccount(
                accountName = "Custom Main",
                accountPayload = CreateUserDefinedFiatAccountPayload(accountData = "IBAN: DE89"),
            )

        val dto = domain.toDto()

        assertIs<CreateUserDefinedFiatAccountDto>(dto)
        assertEquals(FiatPaymentRailDto.CUSTOM, dto.paymentRail)
        assertEquals("Custom Main", dto.accountName)
        assertEquals("IBAN: DE89", dto.accountPayload.accountData)
    }
}
