package network.bisq.mobile.data.replicated.api.dto.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class UserDefinedFiatAccountPayloadDto(
    val accountData: String,
) : FiatAccountPayloadDto {
    companion object {
        const val MAX_DATA_LENGTH = 1000
    }

    init {
        verify()
    }

    fun verify() {
        require(accountData.length <= MAX_DATA_LENGTH) { "Account data exceeds max length" }
        require(accountData.isNotBlank()) { "Account data cannot be blank" }
    }
}
