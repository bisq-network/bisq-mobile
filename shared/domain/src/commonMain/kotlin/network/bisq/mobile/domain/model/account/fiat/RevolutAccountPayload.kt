package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.data.replicated.account.payment_method.FiatPaymentRailUtil
import network.bisq.mobile.data.replicated.common.validation.NetworkDataValidation
import network.bisq.mobile.data.replicated.common.validation.PaymentAccountValidation

data class RevolutAccountPayload(
    val userName: String,
    val selectedCurrencyCodes: List<String>,
) : FiatAccountPayload {
    companion object {
        const val USER_NAME_MIN_LENGTH = 2
        const val USER_NAME_MAX_LENGTH = 70
    }

    init {
        verify()
    }

    fun verify() {
        NetworkDataValidation.validateRequiredText(userName, USER_NAME_MIN_LENGTH, USER_NAME_MAX_LENGTH)
        PaymentAccountValidation.validateCurrencyCodes(
            selectedCurrencyCodes,
            FiatPaymentRailUtil.revolutCurrencies,
            "Revolut currency codes",
        )
    }
}
