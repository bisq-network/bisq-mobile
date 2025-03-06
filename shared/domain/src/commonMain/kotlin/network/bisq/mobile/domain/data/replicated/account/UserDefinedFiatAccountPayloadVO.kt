package network.bisq.mobile.domain.data.replicated.account

data class UserDefinedFiatAccountPayloadVO(
    override val id: String,
    override val paymentMethodName: String,
    val accountData: String
) : AccountPayloadVO(id, paymentMethodName) {

    companion object {
        const val MAX_DATA_LENGTH = 1000
    }

    init {
        require(accountData.length <= MAX_DATA_LENGTH) { "Account data exceeds max length" }
    }

    override fun verify() {
        require(accountData.isNotBlank()) { "Account data cannot be blank" }
    }
}
