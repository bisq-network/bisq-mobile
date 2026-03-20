package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.data.replicated.common.validation.NetworkDataValidation

data class WeChatPayAccountPayload(
    val accountNr: String,
    val countryCode: String = "CN",
) : FiatAccountPayload {
    companion object {
        const val ACCOUNT_NR_MIN_LENGTH = 1
        const val ACCOUNT_NR_MAX_LENGTH = 50
    }

    init {
        verify()
    }

    fun verify() {
        NetworkDataValidation.validateCode(countryCode)
        require(countryCode == "CN") { "Country code must be 'CN' for WeChatPay accounts" }
        NetworkDataValidation.validateRequiredText(accountNr, ACCOUNT_NR_MIN_LENGTH, ACCOUNT_NR_MAX_LENGTH)
    }
}
