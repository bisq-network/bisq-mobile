package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.domain.model.account.PaymentAccountPayload

data class UserDefinedFiatAccountPayload(
    val accountData: String,
) : PaymentAccountPayload {
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
