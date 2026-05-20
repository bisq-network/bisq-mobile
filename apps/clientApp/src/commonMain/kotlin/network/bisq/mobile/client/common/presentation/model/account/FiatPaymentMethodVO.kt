package network.bisq.mobile.client.common.presentation.model.account

import network.bisq.mobile.data.replicated.account.payment_method.FiatPaymentRail
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethod
import network.bisq.mobile.domain.utils.getLogger
import network.bisq.mobile.i18n.i18n

data class FiatPaymentMethodVO(
    override val paymentType: PaymentTypeVO,
    override val name: String,
    val supportedCurrencyCodes: String,
    val countryNames: String,
    val chargebackRisk: FiatPaymentMethodChargebackRiskVO?,
    override val tradeLimitInfo: String,
    override val tradeDuration: String,
) : PaymentMethodVO

fun FiatPaymentMethod.toVO(): FiatPaymentMethodVO? =
    paymentRail.toPaymentTypeVO()?.let { paymentType ->
        FiatPaymentMethodVO(
            paymentType = paymentType,
            name = name,
            supportedCurrencyCodes = supportedCurrencies.joinToString(", ") { it.code },
            countryNames =
                if (matchesAllCountries) {
                    "paymentAccounts.allCountries".i18n()
                } else {
                    supportedCountries.joinToString(", ") { it.name }
                },
            chargebackRisk = chargebackRisk.toVO(),
            tradeLimitInfo = tradeLimitInfo,
            tradeDuration = tradeDuration,
        )
    }

private val log = getLogger("FiatPaymentMethodVO")

fun FiatPaymentRail.toPaymentTypeVO(): PaymentTypeVO? =
    runCatching { PaymentTypeVO.valueOf(name) }
        .onFailure { e -> log.w(e) { "Unknown payment rail '$name' -> paymentType is null" } }
        .getOrNull()
