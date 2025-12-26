package network.bisq.mobile.domain.service.accounts

import network.bisq.mobile.domain.data.replicated.account.fiat.FiatAccountVO

data class AccountsState(
    val accounts: List<FiatAccountVO> = emptyList(),
    val selectedAccountIndex: Int = -1,
)
