package network.bisq.mobile.data.service.accounts

import network.bisq.mobile.data.replicated.api.dto.account.fiat.FiatAccountDto

data class AccountsState(
    val accounts: List<FiatAccountDto> = emptyList(),
    val selectedAccountIndex: Int = -1,
)
