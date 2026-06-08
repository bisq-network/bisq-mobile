package network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.bank

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccount
import network.bisq.mobile.client.payment_accounts.domain.service.PaymentAccountsServiceFacade
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.main.MainPresenter

open class BankAccountDetailPresenter(
    private val paymentAccountsServiceFacade: PaymentAccountsServiceFacade,
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter) {
    private val _uiState = MutableStateFlow(BankAccountDetailUiState())
    val uiState: StateFlow<BankAccountDetailUiState> = _uiState.asStateFlow()

    fun initialize(account: BankAccount) {
        val countryCode = account.accountPayload.country.code
        presenterScope.launch {
            _uiState.update {
                it.copy(
                    countryDetails = null,
                    isLoadingCountryDetails = true,
                    isCountryDetailsError = false,
                )
            }

            paymentAccountsServiceFacade
                .getBankAccountCountryDetails(countryCode)
                .onSuccess { details ->
                    _uiState.update {
                        it.copy(
                            countryDetails = details,
                            isLoadingCountryDetails = false,
                            isCountryDetailsError = false,
                        )
                    }
                }.onFailure { error ->
                    log.e(error) { "Failed to load bank account country details for $countryCode" }
                    _uiState.update {
                        it.copy(
                            countryDetails = null,
                            isLoadingCountryDetails = false,
                            isCountryDetailsError = true,
                        )
                    }
                }
        }
    }
}
