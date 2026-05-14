package network.bisq.mobile.data.mapping.account.fiat

import network.bisq.mobile.data.model.account.fiat.FiatPaymentRailDto
import network.bisq.mobile.data.model.account.fiat.create.CreateUserDefinedFiatAccountDto
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
