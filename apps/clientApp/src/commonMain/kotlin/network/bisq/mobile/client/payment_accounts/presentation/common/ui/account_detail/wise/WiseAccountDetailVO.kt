package network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.wise

import network.bisq.mobile.client.common.presentation.model.account.FiatPaymentMethodChargebackRiskVO
import network.bisq.mobile.client.common.presentation.model.account.toVO
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.wise.WiseAccount
import network.bisq.mobile.client.payment_accounts.presentation.common.util.toDisplayString

data class WiseAccountDetailVO(
    val chargebackRisk: FiatPaymentMethodChargebackRiskVO?,
    val selectedCurrencies: String,
)

fun WiseAccount.toDetailVO(): WiseAccountDetailVO =
    WiseAccountDetailVO(
        chargebackRisk = accountPayload.chargebackRisk?.toVO(),
        selectedCurrencies = accountPayload.selectedCurrencies.sortedBy { it.code }.joinToString { it.toDisplayString() },
    )
