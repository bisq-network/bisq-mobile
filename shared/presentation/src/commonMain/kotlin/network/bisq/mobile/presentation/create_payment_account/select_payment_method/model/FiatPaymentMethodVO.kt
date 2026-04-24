package network.bisq.mobile.presentation.create_payment_account.select_payment_method.model

import network.bisq.mobile.data.replicated.account.payment_method.FiatPaymentRail
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethod
import network.bisq.mobile.domain.utils.getLogger
import network.bisq.mobile.presentation.common.model.account.FiatPaymentMethodChargebackRiskVO
import network.bisq.mobile.presentation.common.model.account.PaymentTypeVO
import network.bisq.mobile.presentation.common.model.account.toVO

data class FiatPaymentMethodVO(
    override val paymentType: PaymentTypeVO,
    override val name: String,
    val supportedCurrencyCodes: String,
    val countryNames: String,
    val chargebackRisk: FiatPaymentMethodChargebackRiskVO?,
) : PaymentMethodVO

fun FiatPaymentMethod.toVO(): FiatPaymentMethodVO? =
    paymentRail.toPaymentTypeVO()?.let { paymentType ->
        FiatPaymentMethodVO(
            paymentType = paymentType,
            name = name,
            supportedCurrencyCodes = supportedCurrencyCodes,
            countryNames = countryNames,
            chargebackRisk = chargebackRisk.toVO(),
        )
    }

private val log = getLogger("FiatPaymentMethodVO")

fun FiatPaymentRail.toPaymentTypeVO(): PaymentTypeVO? =
    runCatching { PaymentTypeVO.valueOf(name) }
        .onFailure { e -> log.w(e) { "Unknown payment rail '$name' -> paymentType is null" } }
        .getOrNull()
