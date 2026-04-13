package network.bisq.mobile.data.service.accounts

import network.bisq.mobile.domain.model.account.fiat.UserDefinedFiatAccount

data class AccountsState(
    val accounts: List<UserDefinedFiatAccount> = emptyList(),
    val selectedAccountIndex: Int = 0,
)
