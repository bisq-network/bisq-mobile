package network.bisq.mobile.client.payment_accounts.data.mapping.fiat

import network.bisq.mobile.client.payment_accounts.data.model.fiat.payment_method.FiatPaymentMethodChargebackRiskDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.payment_method.FiatPaymentMethodDto
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.FiatPaymentMethod
import network.bisq.mobile.data.replicated.account.payment_method.FiatPaymentRail
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk

fun FiatPaymentMethodDto.toDomain(): FiatPaymentMethod =
    FiatPaymentMethod(
        paymentRail = FiatPaymentRail.valueOf(paymentRail.name),
        name = name,
        supportedCurrencies = supportedCurrencies.map { it.toDomain() },
        supportedCountries = supportedCountries.map { it.toDomain() },
        matchesAllCountries = matchesAllCountries,
        chargebackRisk = chargebackRisk.toDomain(),
        tradeLimitInfo = tradeLimitInfo,
        tradeDuration = tradeDuration,
    )

fun FiatPaymentMethodChargebackRiskDto.toDomain(): FiatPaymentMethodChargebackRisk = FiatPaymentMethodChargebackRisk.valueOf(name)
