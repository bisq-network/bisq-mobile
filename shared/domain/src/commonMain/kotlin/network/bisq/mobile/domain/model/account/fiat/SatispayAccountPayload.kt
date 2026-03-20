package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.data.replicated.common.validation.PaymentAccountValidation

data class SatispayAccountPayload(
    val holderName: String,
    val mobileNr: String,
) : FiatAccountPayload {
    init {
        verify()
    }

    fun verify() {
        PaymentAccountValidation.validateHolderName(holderName)
        require(mobileNr.isNotBlank()) { "mobileNr must not be blank" }
    }
}
