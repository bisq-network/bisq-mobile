package network.bisq.mobile.client.create_payment_account.account_review.ui.revolut

import network.bisq.mobile.client.common.presentation.model.account.FiatPaymentMethodChargebackRiskVO
import network.bisq.mobile.client.common.presentation.model.account.toVO
import network.bisq.mobile.client.create_payment_account.core.util.toDisplayString
import network.bisq.mobile.domain.model.account.fiat.RevolutAccount

data class RevolutAccountDetailVO(
    val chargebackRisk: FiatPaymentMethodChargebackRiskVO?,
    val selectedCurrencies: String,
)

fun RevolutAccount.toDetailVO(): RevolutAccountDetailVO =
    RevolutAccountDetailVO(
        chargebackRisk = accountPayload.chargebackRisk?.toVO(),
        selectedCurrencies = accountPayload.selectedCurrencies.sortedBy { it.code }.joinToString { it.toDisplayString() },
    )
