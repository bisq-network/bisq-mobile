package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.data.replicated.common.validation.NetworkDataValidation

data class AchTransferAccountPayload(
    val holderName: String?,
    val holderAddress: String,
    val bankName: String?,
    val bankId: String?,
    val accountNr: String,
    val bankAccountType: BankAccountType?,
    val holderId: String? = null,
    val branchId: String? = null,
    val nationalAccountId: String? = null,
) : FiatAccountPayload {
    companion object {
        const val HOLDER_ADDRESS_MIN_LENGTH = 5
        const val HOLDER_ADDRESS_MAX_LENGTH = 150
    }

    init {
        verify()
    }

    fun verify() {
        BankAccountPayloadValidation.validateCommon(
            "US",
            "USD",
            holderName,
            holderId,
            bankName,
            bankId,
            branchId,
            accountNr,
            nationalAccountId,
        )
        NetworkDataValidation.validateRequiredText(holderAddress, HOLDER_ADDRESS_MIN_LENGTH, HOLDER_ADDRESS_MAX_LENGTH)
        require(holderName != null) { "holderName is required for ACH_TRANSFER" }
        require(bankName != null) { "bankName is required for ACH_TRANSFER" }
        require(bankId != null) { "bankId is required for ACH_TRANSFER" }
        require(bankAccountType != null) { "bankAccountType is required for ACH_TRANSFER" }
    }
}
