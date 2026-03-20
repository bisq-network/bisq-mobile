package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.data.replicated.common.validation.NetworkDataValidation

data class AliPayAccountPayload(
    val accountNr: String,
) : FiatAccountPayload {
    companion object {
        const val ACCOUNT_NR_MIN_LENGTH = 2
        const val ACCOUNT_NR_MAX_LENGTH = 100
    }

    init {
        verify()
    }

    fun verify() {
        NetworkDataValidation.validateCode("CN")
        NetworkDataValidation.validateRequiredText(accountNr, ACCOUNT_NR_MIN_LENGTH, ACCOUNT_NR_MAX_LENGTH)
    }
}
