package network.bisq.mobile.presentation.settings.payment_accounts_musig

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bisq.mobile.data.service.accounts.PaymentAccountsServiceFacade
import network.bisq.mobile.domain.model.account.PaymentAccount
import network.bisq.mobile.domain.model.account.crypto.CryptoPaymentAccount
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentAccount
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.navigation.types.PaymentAccountType
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.settings.payment_accounts_musig.model.toVO

open class PaymentAccountsMusigPresenter(
    private val paymentAccountsServiceFacade: PaymentAccountsServiceFacade,
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter) {
    private val _uiState =
        MutableStateFlow(
            PaymentAccountsMusigUiState(),
        )
    val uiState: StateFlow<PaymentAccountsMusigUiState> = _uiState.asStateFlow()

    override fun onViewAttached() {
        super.onViewAttached()
        loadAccounts()
        observeAccounts()
    }

    fun onAction(action: PaymentAccountsMusigUiAction) {
        when (action) {
            is PaymentAccountsMusigUiAction.OnDeleteAccountClick -> onDeleteAccountClick()
            is PaymentAccountsMusigUiAction.OnCancelDeleteAccountClick -> onCancelDeleteAccountClick()
            is PaymentAccountsMusigUiAction.OnConfirmDeleteAccountClick -> onConfirmDeleteAccountClick()
            is PaymentAccountsMusigUiAction.OnSaveAccountClick -> onSaveAccountClick()
            is PaymentAccountsMusigUiAction.OnRetryLoadAccountsClick -> onRetryLoadAccountsClick()
            is PaymentAccountsMusigUiAction.OnAccountSelect -> onAccountSelect(action.index)
            is PaymentAccountsMusigUiAction.OnEditAccountClick -> onEditAccountClick()
            is PaymentAccountsMusigUiAction.OnTabSelect -> onTabSelect(action.tab)
            PaymentAccountsMusigUiAction.OnAddCryptoAccountClick -> onAddCryptoAccountClick()
            PaymentAccountsMusigUiAction.OnAddFiatAccountClick -> onAddFiatAccountClick()
        }
    }

    private fun onAddCryptoAccountClick() {
        navigateTo(NavRoute.CreatePaymentAccount(accountType = PaymentAccountType.CRYPTO))
    }

    private fun onAddFiatAccountClick() {
        navigateTo(NavRoute.CreatePaymentAccount(accountType = PaymentAccountType.FIAT))
    }

    private fun onTabSelect(tab: PaymentAccountTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    private fun loadAccounts() {
        presenterScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingAccounts = true,
                    isLoadingAccountsError = false,
                )
            }

            paymentAccountsServiceFacade
                .getAccounts()
                .onFailure { error ->
                    log.e(error) { "Failed to load accounts" }
                    _uiState.update {
                        it.copy(
                            isLoadingAccountsError = true,
                            isLoadingAccounts = false,
                        )
                    }
                    return@launch
                }

            _uiState.update { it.copy(isLoadingAccounts = false) }
        }
    }

    private fun observeAccounts() {
        presenterScope.launch {
            paymentAccountsServiceFacade.accounts.collect { accounts ->
                _uiState.update { state ->
                    state.copy(
                        fiatAccounts = getFiatAccounts(accounts).mapNotNull { it.toVO() },
                        cryptoAccounts = getCryptoAccounts(accounts).mapNotNull { it.toVO() },
                    )
                }
            }
        }
    }

    private fun onRetryLoadAccountsClick() {
        loadAccounts()
    }

    private fun onEditAccountClick() {
    }

    private fun onSaveAccountClick() {
    }

    private fun onDeleteAccountClick() {
    }

    private fun onAccountSelect(index: Int) {
    }

    private fun onCancelDeleteAccountClick() {
    }

    private fun onConfirmDeleteAccountClick() {
    }

    private fun getFiatAccounts(accounts: List<PaymentAccount>): List<FiatPaymentAccount> = accounts.filterIsInstance<FiatPaymentAccount>()

    private fun getCryptoAccounts(accounts: List<PaymentAccount>): List<CryptoPaymentAccount> = accounts.filterIsInstance<CryptoPaymentAccount>()
}
