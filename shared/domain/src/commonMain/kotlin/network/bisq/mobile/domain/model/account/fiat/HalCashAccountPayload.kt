package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.data.replicated.common.validation.NetworkDataValidation

data class HalCashAccountPayload(
    val mobileNr: String,
) : FiatAccountPayload {
    init {
        verify()
    }

    fun verify() {
        NetworkDataValidation.validateCode("ES")
        require(mobileNr.isNotBlank()) { "mobileNr must not be blank" }
    }
}
