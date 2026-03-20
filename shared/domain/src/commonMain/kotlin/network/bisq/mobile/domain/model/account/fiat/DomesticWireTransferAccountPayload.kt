package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.data.replicated.common.validation.NetworkDataValidation

data class DomesticWireTransferAccountPayload(
    val holderName: String?,
    val holderAddress: String,
    val bankName: String?,
    val bankId: String?,
    val accountNr: String,
    val holderId: String? = null,
    val branchId: String? = null,
    val bankAccountType: BankAccountType? = null,
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
        require(holderName != null) { "holderName is required for DOMESTIC_WIRE_TRANSFER" }
        require(bankName != null) { "bankName is required for DOMESTIC_WIRE_TRANSFER" }
        require(bankId != null) { "bankId is required for DOMESTIC_WIRE_TRANSFER" }
    }
}
