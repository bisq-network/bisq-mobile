package network.bisq.mobile.presentation.create_payment_account.account_review.ui.wise

import network.bisq.mobile.domain.model.account.fiat.WiseAccount
import network.bisq.mobile.presentation.common.model.account.FiatPaymentMethodChargebackRiskVO
import network.bisq.mobile.presentation.common.model.account.toVO
import network.bisq.mobile.presentation.create_payment_account.core.util.fiatCurrencyCodeNameToDisplayStringFormat

data class WiseAccountDetailVO(
    val chargebackRisk: FiatPaymentMethodChargebackRiskVO?,
    val selectedCurrencies: String,
)

fun WiseAccount.toDetailVO(): WiseAccountDetailVO =
    WiseAccountDetailVO(
        chargebackRisk = accountPayload.chargebackRisk?.toVO(),
        selectedCurrencies = accountPayload.selectedCurrencies.sortedBy { it.code }.joinToString { fiatCurrencyCodeNameToDisplayStringFormat(it.code, it.name) },
    )
