package network.bisq.mobile.client.create_payment_account.account_review.ui.wise

import network.bisq.mobile.client.common.presentation.model.account.FiatPaymentMethodChargebackRiskVO
import network.bisq.mobile.client.common.presentation.model.account.toVO
import network.bisq.mobile.client.create_payment_account.core.util.fiatCurrencyCodeNameToDisplayStringFormat
import network.bisq.mobile.domain.model.account.fiat.WiseAccount

data class WiseAccountDetailVO(
    val chargebackRisk: FiatPaymentMethodChargebackRiskVO?,
    val selectedCurrencies: String,
)

fun WiseAccount.toDetailVO(): WiseAccountDetailVO =
    WiseAccountDetailVO(
        chargebackRisk = accountPayload.chargebackRisk?.toVO(),
        selectedCurrencies = accountPayload.selectedCurrencies.sortedBy { it.code }.joinToString { fiatCurrencyCodeNameToDisplayStringFormat(it.code, it.name) },
    )
