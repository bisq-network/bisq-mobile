package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.data.replicated.common.validation.EmailValidation
import network.bisq.mobile.data.replicated.common.validation.NetworkDataValidation
import network.bisq.mobile.data.replicated.common.validation.PaymentAccountValidation

data class MoneyGramAccountPayload(
    val countryCode: String,
    val selectedCurrencyCodes: List<String>,
    val holderName: String,
    val email: String,
    val state: String,
) : FiatAccountPayload {
    init {
        verify()
    }

    fun verify() {
        NetworkDataValidation.validateCode(countryCode)
        PaymentAccountValidation.validateCurrencyCodes(selectedCurrencyCodes)
        require(EmailValidation.isValid(email)) { "Email is invalid" }
        PaymentAccountValidation.validateHolderName(holderName)
    }
}
