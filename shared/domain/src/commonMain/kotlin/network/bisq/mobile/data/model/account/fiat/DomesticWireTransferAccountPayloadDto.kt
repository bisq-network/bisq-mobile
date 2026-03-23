package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class DomesticWireTransferAccountPayloadDto(
    val holderName: String?,
    val holderAddress: String,
    val bankName: String?,
    val bankId: String?,
    val accountNr: String,
    val holderId: String? = null,
    val branchId: String? = null,
    val bankAccountType: BankAccountTypeDto? = null,
    val nationalAccountId: String? = null,
) : FiatAccountPayloadDto
