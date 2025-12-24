package network.bisq.mobile.domain.service.accounts

import network.bisq.mobile.domain.data.replicated.account.UserDefinedFiatAccountVO

data class AccountsState(
    val accounts: List<UserDefinedFiatAccountVO> = emptyList(),
    val selectedAccountIndex: Int = -1
)