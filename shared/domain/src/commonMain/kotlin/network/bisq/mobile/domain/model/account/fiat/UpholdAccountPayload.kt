package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.data.replicated.common.validation.PaymentAccountValidation

data class UpholdAccountPayload(
    val selectedCurrencyCodes: List<String>,
    val holderName: String,
    val accountId: String,
) : FiatAccountPayload {
    init {
        verify()
    }

    fun verify() {
        PaymentAccountValidation.validateCurrencyCodes(selectedCurrencyCodes)
        require(accountId.isNotBlank()) { "Account ID cannot be blank" }
        // Holder name intentionally not validated (optional in Bisq 1/2)
    }
}
