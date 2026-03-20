package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.data.replicated.common.validation.NetworkDataValidation
import network.bisq.mobile.data.replicated.common.validation.PaymentAccountValidation

data class NeftAccountPayload(
    val holderName: String,
    val accountNr: String,
    val ifsc: String,
) : FiatAccountPayload {
    companion object {
        const val ACCOUNT_NR_MIN_LENGTH = 1
        const val ACCOUNT_NR_MAX_LENGTH = 50
        const val IFSC_MIN_LENGTH = 1
        const val IFSC_MAX_LENGTH = 50
    }

    init {
        verify()
    }

    fun verify() {
        NetworkDataValidation.validateCode("IN")
        PaymentAccountValidation.validateHolderName(holderName)
        NetworkDataValidation.validateRequiredText(accountNr, ACCOUNT_NR_MIN_LENGTH, ACCOUNT_NR_MAX_LENGTH)
        NetworkDataValidation.validateRequiredText(ifsc, IFSC_MIN_LENGTH, IFSC_MAX_LENGTH)
    }
}
