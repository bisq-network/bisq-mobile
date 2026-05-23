package network.bisq.mobile.data.mapping.account.fiat

import network.bisq.mobile.data.model.account.fiat.FiatCurrencyDto
import network.bisq.mobile.data.model.account.fiat.FiatPaymentMethodChargebackRiskDto
import network.bisq.mobile.data.model.account.fiat.FiatPaymentRailDto
import network.bisq.mobile.data.model.account.fiat.RevolutAccountDto
import network.bisq.mobile.data.model.account.fiat.RevolutAccountPayloadDto
import network.bisq.mobile.data.model.account.fiat.create.CreateRevolutAccountDto
import network.bisq.mobile.domain.model.account.create.fiat.CreateRevolutAccount
import network.bisq.mobile.domain.model.account.create.fiat.CreateRevolutAccountPayload
import network.bisq.mobile.domain.model.account.fiat.FiatCurrency
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RevolutAccountMappingTest {
    @Test
    fun `toDomain maps all RevolutAccountDto fields correctly`() {
        val dto =
            RevolutAccountDto(
                accountName = "Revolut Main",
                accountPayload =
                    RevolutAccountPayloadDto(
                        selectedCurrencies = listOf(FiatCurrencyDto(code = "USD", name = "US Dollar"), FiatCurrencyDto(code = "EUR", name = "Euro")),
                        userName = "satoshi",
                        chargebackRisk = FiatPaymentMethodChargebackRiskDto.MODERATE,
                        paymentMethodName = "Revolut",
                    ),
                creationDate = "2026-05-10",
                tradeLimitInfo = "5000.00",
                tradeDuration = "4 days",
            )

        val domain = dto.toDomain()

        assertEquals("Revolut Main", domain.accountName)
        assertEquals(listOf(FiatCurrency(code = "USD", name = "US Dollar"), FiatCurrency(code = "EUR", name = "Euro")), domain.accountPayload.selectedCurrencies)
        assertEquals("satoshi", domain.accountPayload.userName)
        assertEquals(FiatPaymentMethodChargebackRisk.MODERATE, domain.accountPayload.chargebackRisk)
        assertEquals("Revolut", domain.accountPayload.paymentMethodName)
        assertEquals("2026-05-10", domain.creationDate)
        assertEquals("5000.00", domain.tradeLimitInfo)
        assertEquals("4 days", domain.tradeDuration)
    }

    @Test
    fun `toDto maps create RevolutAccount fields correctly`() {
        val domain =
            CreateRevolutAccount(
                accountName = "Revolut Main",
                accountPayload = CreateRevolutAccountPayload(userName = "alice", selectedCurrencies = listOf(FiatCurrency(code = "USD", name = "US Dollar"), FiatCurrency(code = "EUR", name = "Euro"))),
            )

        val dto = domain.toDto()

        assertIs<CreateRevolutAccountDto>(dto)
        assertEquals(FiatPaymentRailDto.REVOLUT, dto.paymentRail)
        assertEquals("Revolut Main", dto.accountName)
        assertEquals("alice", dto.accountPayload.userName)
        assertEquals(listOf("USD", "EUR"), dto.accountPayload.selectedCurrencyCodes)
    }
}
