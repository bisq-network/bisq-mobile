package network.bisq.mobile.client.payment_accounts.data.mapping.fiat

import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.BankAccountTypeDto
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccountType

fun BankAccountTypeDto.toDomain(): BankAccountType = BankAccountType.valueOf(name)

fun BankAccountType.toDto(): BankAccountTypeDto = BankAccountTypeDto.valueOf(name)
