package network.bisq.mobile.domain.model.account.fiat

data class UserDefinedFiatAccountPayload(
    val accountData: String,
) : FiatAccountPayload {
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
