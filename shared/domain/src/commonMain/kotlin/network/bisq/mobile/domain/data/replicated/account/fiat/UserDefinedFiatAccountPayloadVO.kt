package network.bisq.mobile.domain.data.replicated.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class UserDefinedFiatAccountPayloadVO(
    val accountData: String,
) : FiatAccountPayloadVO {
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
