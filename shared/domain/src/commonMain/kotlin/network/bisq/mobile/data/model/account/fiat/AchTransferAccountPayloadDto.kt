package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class AchTransferAccountPayloadDto(
    val holderName: String?,
    val holderAddress: String,
    val bankName: String?,
    val bankId: String?,
    val accountNr: String,
    val bankAccountType: BankAccountTypeDto?,
    val holderId: String? = null,
    val branchId: String? = null,
    val nationalAccountId: String? = null,
) : FiatAccountPayloadDto
