package network.bisq.mobile.data.mapping.account.fiat

import network.bisq.mobile.data.model.account.fiat.FiatCurrencyDto
import network.bisq.mobile.data.model.account.fiat.FiatPaymentMethodChargebackRiskDto
import network.bisq.mobile.data.model.account.fiat.FiatPaymentRailDto
import network.bisq.mobile.data.model.account.fiat.WiseAccountDto
import network.bisq.mobile.data.model.account.fiat.WiseAccountPayloadDto
import network.bisq.mobile.data.model.account.fiat.create.CreateWiseAccountDto
import network.bisq.mobile.domain.model.account.create.fiat.CreateWiseAccount
import network.bisq.mobile.domain.model.account.create.fiat.CreateWiseAccountPayload
import network.bisq.mobile.domain.model.account.fiat.FiatCurrency
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class WiseAccountMappingTest {
    @Test
    fun `toDomain maps all WiseAccountDto fields correctly`() {
        val dto =
            WiseAccountDto(
                accountName = "Wise Main",
                accountPayload =
                    WiseAccountPayloadDto(
                        selectedCurrencies = listOf(FiatCurrencyDto(code = "USD", name = "US Dollar"), FiatCurrencyDto(code = "EUR", name = "Euro")),
                        holderName = "Satoshi Nakamoto",
                        email = "satoshi@example.com",
                        chargebackRisk = FiatPaymentMethodChargebackRiskDto.MODERATE,
                        paymentMethodName = "Wise",
                    ),
                creationDate = "2026-05-10",
                tradeLimitInfo = "5000.00",
                tradeDuration = "4 days",
            )

        val domain = dto.toDomain()

        assertEquals("Wise Main", domain.accountName)
        assertEquals(listOf(FiatCurrency(code = "USD", name = "US Dollar"), FiatCurrency(code = "EUR", name = "Euro")), domain.accountPayload.selectedCurrencies)
        assertEquals("Satoshi Nakamoto", domain.accountPayload.holderName)
        assertEquals("satoshi@example.com", domain.accountPayload.email)
        assertEquals(FiatPaymentMethodChargebackRisk.MODERATE, domain.accountPayload.chargebackRisk)
        assertEquals("Wise", domain.accountPayload.paymentMethodName)
        assertEquals("2026-05-10", domain.creationDate)
        assertEquals("5000.00", domain.tradeLimitInfo)
        assertEquals("4 days", domain.tradeDuration)
    }

    @Test
    fun `toDto maps create WiseAccount fields correctly`() {
        val domain =
            CreateWiseAccount(
                accountName = "Wise Main",
                accountPayload = CreateWiseAccountPayload(selectedCurrencies = listOf(FiatCurrency(code = "USD", name = "US Dollar"), FiatCurrency(code = "EUR", name = "Euro")), holderName = "Alice", email = "alice@example.com"),
            )

        val dto = domain.toDto()

        assertIs<CreateWiseAccountDto>(dto)
        assertEquals(FiatPaymentRailDto.WISE, dto.paymentRail)
        assertEquals("Wise Main", dto.accountName)
        assertEquals(listOf("USD", "EUR"), dto.accountPayload.selectedCurrencyCodes)
        assertEquals("Alice", dto.accountPayload.holderName)
        assertEquals("alice@example.com", dto.accountPayload.email)
    }
}
