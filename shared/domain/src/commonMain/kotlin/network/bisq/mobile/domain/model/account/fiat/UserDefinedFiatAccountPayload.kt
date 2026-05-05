package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.domain.utils.EMPTY_STRING

data class UserDefinedFiatAccountPayload(
    val accountData: String,
    override val chargebackRisk: FiatPaymentMethodChargebackRisk? = null,
    override val paymentMethodName: String = EMPTY_STRING,
    override val currency: String = EMPTY_STRING,
    override val country: String? = null,
) : FiatPaymentAccountPayload {
    companion object {
        const val MAX_DATA_LENGTH = 1000
    }

    init {
        require(accountData.length <= MAX_DATA_LENGTH) { "Account data exceeds max length" }
    }

    fun verify() {
        require(accountData.isNotBlank()) { "Account data cannot be blank" }
    }
}
