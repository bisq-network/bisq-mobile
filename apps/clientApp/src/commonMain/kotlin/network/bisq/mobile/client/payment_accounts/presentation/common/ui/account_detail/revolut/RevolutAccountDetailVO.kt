package network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.revolut

import network.bisq.mobile.client.common.presentation.model.account.FiatPaymentMethodChargebackRiskVO
import network.bisq.mobile.client.common.presentation.model.account.toVO
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.revolut.RevolutAccount
import network.bisq.mobile.client.payment_accounts.presentation.common.util.toDisplayString

data class RevolutAccountDetailVO(
    val chargebackRisk: FiatPaymentMethodChargebackRiskVO?,
    val selectedCurrencies: String,
)

fun RevolutAccount.toDetailVO(): RevolutAccountDetailVO =
    RevolutAccountDetailVO(
        chargebackRisk = accountPayload.chargebackRisk?.toVO(),
        selectedCurrencies = accountPayload.selectedCurrencies.sortedBy { it.code }.joinToString { it.toDisplayString() },
    )
