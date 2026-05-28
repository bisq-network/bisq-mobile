package network.bisq.mobile.client.payment_accounts.data.model.fiat.ach_transfer

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.payment_accounts.data.model.create.CreatePaymentAccountPayloadDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.BankAccountTypeDto

@Serializable
data class CreateAchTransferAccountPayloadDto(
    val holderName: String,
    val holderAddress: String,
    val bankName: String,
    val routingNr: String,
    val accountNr: String,
    val bankAccountType: BankAccountTypeDto,
) : CreatePaymentAccountPayloadDto
