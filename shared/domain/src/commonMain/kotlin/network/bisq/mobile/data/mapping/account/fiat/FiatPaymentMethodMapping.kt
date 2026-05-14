package network.bisq.mobile.data.mapping.account.fiat

import network.bisq.mobile.data.model.account.fiat.FiatPaymentMethodChargebackRiskDto
import network.bisq.mobile.data.model.account.fiat.FiatPaymentMethodDto
import network.bisq.mobile.data.replicated.account.payment_method.FiatPaymentRail
import network.bisq.mobile.domain.model.account.fiat.Country
import network.bisq.mobile.domain.model.account.fiat.FiatCurrency
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethod
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk

fun FiatPaymentMethodDto.toDomain(): FiatPaymentMethod =
    FiatPaymentMethod(
        paymentRail = FiatPaymentRail.valueOf(paymentRail.name),
        name = name,
        supportedCurrencies = supportedCurrencies.map { FiatCurrency(code = it.code, name = it.name) },
        supportedCountries = supportedCountries.map { Country(code = it.code, name = it.name) },
        matchesAllCountries = matchesAllCountries,
        chargebackRisk = chargebackRisk.toDomain(),
        tradeLimitInfo = tradeLimitInfo,
        tradeDuration = tradeDuration,
    )

fun FiatPaymentMethodChargebackRiskDto.toDomain(): FiatPaymentMethodChargebackRisk = FiatPaymentMethodChargebackRisk.valueOf(name)
