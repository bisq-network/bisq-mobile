package network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.cash_deposit

import network.bisq.mobile.domain.model.account.fiat.BankAccountCountryDetails

data class CashDepositAccountDetailUiState(
    val countryDetails: BankAccountCountryDetails? = null,
    val isLoadingCountryDetails: Boolean = false,
    val isCountryDetailsError: Boolean = false,
)
