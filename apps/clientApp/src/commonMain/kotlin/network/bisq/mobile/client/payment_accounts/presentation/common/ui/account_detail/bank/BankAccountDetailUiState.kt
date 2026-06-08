package network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.bank

import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccountCountryDetails

data class BankAccountDetailUiState(
    val countryDetails: BankAccountCountryDetails? = null,
    val isLoadingCountryDetails: Boolean = false,
    val isCountryDetailsError: Boolean = false,
)
