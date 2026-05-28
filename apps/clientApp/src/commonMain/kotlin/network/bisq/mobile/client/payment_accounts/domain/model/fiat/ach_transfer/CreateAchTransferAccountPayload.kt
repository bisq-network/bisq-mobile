package network.bisq.mobile.client.payment_accounts.domain.model.fiat.ach_transfer

import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccountType
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccountPayload

data class CreateAchTransferAccountPayload(
    val holderName: String,
    val holderAddress: String,
    val bankName: String,
    val routingNr: String,
    val accountNr: String,
    val bankAccountType: BankAccountType,
) : CreatePaymentAccountPayload
