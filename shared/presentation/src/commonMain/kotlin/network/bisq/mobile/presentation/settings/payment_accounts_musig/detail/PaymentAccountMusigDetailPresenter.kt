package network.bisq.mobile.presentation.settings.payment_accounts_musig.detail

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bisq.mobile.data.service.accounts.PaymentAccountsServiceFacade
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.main.MainPresenter

class PaymentAccountMusigDetailPresenter(
    private val paymentAccountsServiceFacade: PaymentAccountsServiceFacade,
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter) {
    private val _uiState = MutableStateFlow(PaymentAccountMusigDetailUiState())
    val uiState: StateFlow<PaymentAccountMusigDetailUiState> = _uiState.asStateFlow()

    fun initialize(accountName: String) {
        val paymentAccount = paymentAccountsServiceFacade.accountsByName.value[accountName]
        _uiState.update {
            it.copy(
                paymentAccount = paymentAccount,
                isAccountMissing = paymentAccount == null,
            )
        }
    }

    fun onAction(action: PaymentAccountMusigDetailUiAction) {
        when (action) {
            PaymentAccountMusigDetailUiAction.OnDeleteAccountClick -> onDeleteAccountClick()
            PaymentAccountMusigDetailUiAction.OnConfirmDeleteAccountClick -> onConfirmDeleteAccountClick()
            PaymentAccountMusigDetailUiAction.OnCancelDeleteAccountClick -> onCancelDeleteAccountClick()
        }
    }

    private fun onDeleteAccountClick() {
        _uiState.update { it.copy(showDeleteConfirmationDialog = true) }
    }

    private fun onConfirmDeleteAccountClick() {
        val account = uiState.value.paymentAccount ?: return
        presenterScope.launch {
            showLoading()
            _uiState.update { state -> state.copy(showDeleteConfirmationDialog = false) }
            paymentAccountsServiceFacade
                .deleteAccount(account)
                .onSuccess {
                    navigateBack()
                }.onFailure {
                    handleError(it)
                }
            hideLoading()
        }
    }

    private fun onCancelDeleteAccountClick() {
        _uiState.update { it.copy(showDeleteConfirmationDialog = false) }
    }
}
