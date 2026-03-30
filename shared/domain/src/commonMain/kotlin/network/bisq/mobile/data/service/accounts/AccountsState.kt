package network.bisq.mobile.data.service.accounts

import network.bisq.mobile.domain.model.account.PaymentAccount

data class AccountsState(
    val accounts: List<PaymentAccount> = emptyList(),
    val selectedAccountIndex: Int = -1,
)
