package network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.cash_deposit

import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccountCountryDetails

data class CashDepositAccountDetailUiState(
    val countryDetails: BankAccountCountryDetails? = null,
    val isLoadingCountryDetails: Boolean = false,
    val isCountryDetailsError: Boolean = false,
)
