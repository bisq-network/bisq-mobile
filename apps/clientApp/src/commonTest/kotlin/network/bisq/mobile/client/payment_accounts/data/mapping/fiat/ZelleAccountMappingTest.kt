package network.bisq.mobile.client.payment_accounts.data.mapping.fiat

import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.FiatPaymentRailDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.payment_method.FiatPaymentMethodChargebackRiskDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.zelle.CreateZelleAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.zelle.ZelleAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.zelle.ZelleAccountPayloadDto
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.zelle.CreateZelleAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.zelle.CreateZelleAccountPayload
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ZelleAccountMappingTest {
    @Test
    fun `toDomain maps all ZelleAccountDto fields correctly`() {
        val dto =
            ZelleAccountDto(
                accountName = "Zelle Main",
                accountPayload =
                    ZelleAccountPayloadDto(
                        holderName = "Alice Doe",
                        emailOrMobileNr = "alice@example.com",
                        chargebackRisk = FiatPaymentMethodChargebackRiskDto.MODERATE,
                        paymentMethodName = "Zelle",
                        currency = "USD",
                        country = "United States",
                    ),
                creationDate = "2026-05-10",
                tradeLimitInfo = "1000.00",
                tradeDuration = "8 days",
            )

        val domain = dto.toDomain()

        assertEquals("Zelle Main", domain.accountName)
        assertEquals("Alice Doe", domain.accountPayload.holderName)
        assertEquals("alice@example.com", domain.accountPayload.emailOrMobileNr)
        assertEquals(FiatPaymentMethodChargebackRisk.MODERATE, domain.accountPayload.chargebackRisk)
        assertEquals("Zelle", domain.accountPayload.paymentMethodName)
        assertEquals("USD", domain.accountPayload.currency.code)
        assertEquals("USD", domain.accountPayload.currency.name)
        assertEquals("United States", domain.accountPayload.country.code)
        assertEquals("United States", domain.accountPayload.country.name)
        assertEquals("2026-05-10", domain.creationDate)
        assertEquals("1000.00", domain.tradeLimitInfo)
        assertEquals("8 days", domain.tradeDuration)
    }

    @Test
    fun `toDto maps create ZelleAccount fields correctly`() {
        val domain =
            CreateZelleAccount(
                accountName = "Zelle Main",
                accountPayload = CreateZelleAccountPayload(holderName = "Alice", emailOrMobileNr = "alice@example.com"),
            )

        val dto = domain.toDto()

        assertIs<CreateZelleAccountDto>(dto)
        assertEquals(FiatPaymentRailDto.ZELLE, dto.paymentRail)
        assertEquals("Zelle Main", dto.accountName)
        assertEquals("Alice", dto.accountPayload.holderName)
        assertEquals("alice@example.com", dto.accountPayload.emailOrMobileNr)
    }
}
