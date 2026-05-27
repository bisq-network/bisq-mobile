package network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.cash_deposit

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bisq.mobile.data.service.accounts.PaymentAccountsServiceFacade
import network.bisq.mobile.domain.model.account.fiat.CashDepositAccount
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.main.MainPresenter

open class CashDepositAccountDetailPresenter(
    private val paymentAccountsServiceFacade: PaymentAccountsServiceFacade,
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter) {
    private val _uiState = MutableStateFlow(CashDepositAccountDetailUiState())
    val uiState: StateFlow<CashDepositAccountDetailUiState> = _uiState.asStateFlow()

    fun initialize(account: CashDepositAccount) {
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
