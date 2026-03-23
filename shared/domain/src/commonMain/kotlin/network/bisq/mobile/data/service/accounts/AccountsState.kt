package network.bisq.mobile.data.service.accounts

import network.bisq.mobile.domain.model.account.fiat.FiatAccount

data class AccountsState(
    val accounts: List<FiatAccount> = emptyList(),
    val selectedAccountIndex: Int = -1,
)
