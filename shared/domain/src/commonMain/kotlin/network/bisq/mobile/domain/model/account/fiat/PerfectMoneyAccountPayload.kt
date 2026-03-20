package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.data.replicated.account.payment_method.FiatPaymentRailUtil
import network.bisq.mobile.data.replicated.common.validation.NetworkDataValidation
import network.bisq.mobile.data.replicated.common.validation.PaymentAccountValidation

data class PerfectMoneyAccountPayload(
    val selectedCurrencyCode: String,
    val accountNr: String,
) : FiatAccountPayload {
    companion object {
        const val ACCOUNT_NR_MIN_LENGTH = 1
        const val ACCOUNT_NR_MAX_LENGTH = 50
    }

    init {
        verify()
    }

    fun verify() {
        PaymentAccountValidation.validateCurrencyCodes(
            listOf(selectedCurrencyCode),
            FiatPaymentRailUtil.getPerfectMoneyCurrencyCodes(),
            "Perfect Money currency codes",
        )
        NetworkDataValidation.validateRequiredText(accountNr, ACCOUNT_NR_MIN_LENGTH, ACCOUNT_NR_MAX_LENGTH)
    }
}
