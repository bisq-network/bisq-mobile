package network.bisq.mobile.presentation.create_payment_account.select_payment_method.model

import network.bisq.mobile.data.replicated.account.payment_method.FiatPaymentRail
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethod
import network.bisq.mobile.domain.utils.getLogger
import network.bisq.mobile.presentation.common.model.account.FiatPaymentMethodChargebackRiskVO
import network.bisq.mobile.presentation.common.model.account.PaymentMethodVO
import network.bisq.mobile.presentation.common.model.account.toVO

data class FiatPaymentMethodVO(
    val paymentMethod: PaymentMethodVO,
    val name: String,
    val supportedCurrencyCodes: String,
    val countryNames: String,
    val chargebackRisk: FiatPaymentMethodChargebackRiskVO?,
)

fun FiatPaymentMethod.toVO(): FiatPaymentMethodVO? =
    paymentRail.toPaymentMethodVO()?.let { paymentMethod ->
        FiatPaymentMethodVO(
            paymentMethod = paymentMethod,
            name = name,
            supportedCurrencyCodes = supportedCurrencyCodes,
            countryNames = countryNames,
            chargebackRisk = chargebackRisk.toVO(),
        )
    }

private val log = getLogger("FiatPaymentMethodVO")

fun FiatPaymentRail.toPaymentMethodVO(): PaymentMethodVO? =
    runCatching { PaymentMethodVO.valueOf(name) }
        .onFailure { e -> log.w(e) { "Unknown payment rail '$name' -> paymentMethod is null" } }
        .getOrNull()
