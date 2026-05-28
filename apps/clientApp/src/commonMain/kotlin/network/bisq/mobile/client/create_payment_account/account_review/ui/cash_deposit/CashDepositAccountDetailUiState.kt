package network.bisq.mobile.client.create_payment_account.account_review.ui.cash_deposit

import network.bisq.mobile.domain.model.account.fiat.BankAccountCountryDetails

data class CashDepositAccountDetailUiState(
    val countryDetails: BankAccountCountryDetails? = null,
    val isLoadingCountryDetails: Boolean = false,
    val isCountryDetailsError: Boolean = false,
)
