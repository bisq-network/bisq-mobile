package network.bisq.mobile.client.payment_accounts.data.mapping.fiat

import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.CountryDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.FiatCurrencyDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.FiatPaymentRailDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.payment_method.FiatPaymentMethodChargebackRiskDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.payment_method.FiatPaymentMethodDto
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.Country
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatCurrency
import network.bisq.mobile.data.replicated.account.payment_method.FiatPaymentRail
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk
import kotlin.test.Test
import kotlin.test.assertEquals

class FiatPaymentMethodMappingTest {
    @Test
    fun `toDomain maps all FiatPaymentMethod fields correctly`() {
        // Given
        val dto =
            FiatPaymentMethodDto(
                paymentRail = FiatPaymentRailDto.SEPA,
                name = "SEPA",
                supportedCurrencies = listOf(FiatCurrencyDto(code = "EUR", name = "Euro")),
                supportedCountries =
                    listOf(
                        CountryDto(code = "DE", name = "Germany"),
                        CountryDto(code = "FR", name = "France"),
                    ),
                matchesAllCountries = false,
                chargebackRisk = FiatPaymentMethodChargebackRiskDto.LOW,
                tradeLimitInfo = "1000 EUR",
                tradeDuration = "1-2 business days",
            )

        // When
        val result = dto.toDomain()

        // Then
        assertEquals(FiatPaymentRail.SEPA, result.paymentRail)
        assertEquals("SEPA", result.name)
        assertEquals(listOf(FiatCurrency(code = "EUR", name = "Euro")), result.supportedCurrencies)
        assertEquals(
            listOf(
                Country(code = "DE", name = "Germany"),
                Country(code = "FR", name = "France"),
            ),
            result.supportedCountries,
        )
        assertEquals(false, result.matchesAllCountries)
        assertEquals(FiatPaymentMethodChargebackRisk.LOW, result.chargebackRisk)
        assertEquals("1000 EUR", result.tradeLimitInfo)
        assertEquals("1-2 business days", result.tradeDuration)
    }

    @Test
    fun `chargeback risk toDomain maps all enum values`() {
        // Given / When / Then
        FiatPaymentMethodChargebackRiskDto.entries.forEach { dtoValue ->
            val mapped = dtoValue.toDomain()
            val expected = FiatPaymentMethodChargebackRisk.valueOf(dtoValue.name)
            assertEquals(expected, mapped)
        }
    }
}
