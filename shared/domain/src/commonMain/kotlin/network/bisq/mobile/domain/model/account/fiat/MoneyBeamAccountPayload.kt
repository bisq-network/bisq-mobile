package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.data.replicated.common.validation.NetworkDataValidation
import network.bisq.mobile.data.replicated.common.validation.PaymentAccountValidation

data class MoneyBeamAccountPayload(
    val countryCode: String,
    val holderName: String,
    val emailOrMobileNr: String,
) : FiatAccountPayload {
    init {
        verify()
    }

    fun verify() {
        NetworkDataValidation.validateCode(countryCode)
        PaymentAccountValidation.validateHolderName(holderName)
        require(emailOrMobileNr.isNotBlank()) { "emailOrMobileNr must not be blank" }
    }
}
