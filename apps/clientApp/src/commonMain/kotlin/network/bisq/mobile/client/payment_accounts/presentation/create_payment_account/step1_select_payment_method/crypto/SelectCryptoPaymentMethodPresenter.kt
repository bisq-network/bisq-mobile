package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step1_select_payment_method.crypto

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bisq.mobile.client.common.presentation.model.account.CryptoPaymentMethodVO
import network.bisq.mobile.client.common.presentation.model.account.toVO
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.CryptoPaymentMethod
import network.bisq.mobile.client.payment_accounts.domain.service.PaymentAccountsServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType
import network.bisq.mobile.presentation.main.MainPresenter

class SelectCryptoPaymentMethodPresenter(
    private val paymentAccountsServiceFacade: PaymentAccountsServiceFacade,
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter) {
    private val _uiState =
        MutableStateFlow(
            SelectCryptoPaymentMethodUiState(),
        )
    val uiState: StateFlow<SelectCryptoPaymentMethodUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<SelectCryptoPaymentMethodEffect>()
    val effect = _effect.asSharedFlow()
    private var allCryptoPaymentMethods: List<CryptoPaymentMethodVO> = emptyList()
    private var cryptoPaymentMethodByCode: Map<String, CryptoPaymentMethod> = emptyMap()

    override fun onViewAttached() {
        super.onViewAttached()
        loadPaymentMethods()
    }

    fun loadPaymentMethods() {
        presenterScope.launch {
            _uiState.update { it.copy(isLoading = true, isError = false) }
            paymentAccountsServiceFacade
                .getCryptoPaymentMethods()
                .onSuccess { cryptoPaymentMethods ->
                    cryptoPaymentMethodByCode = cryptoPaymentMethods.associateBy { it.code }
                    allCryptoPaymentMethods = cryptoPaymentMethods.mapNotNull { it.toVO() }
                    val query = _uiState.value.searchQuery
                    val filteredCryptoPaymentMethods = filterCryptoPaymentMethods(query)
                    _uiState.update { state ->
                        val selected = state.selectedPaymentMethod?.takeIf { it in filteredCryptoPaymentMethods }
                        state.copy(
                            paymentMethods = filteredCryptoPaymentMethods,
                            isLoading = false,
                            selectedPaymentMethod = selected,
                        )
                    }
                }.onFailure {
                    _uiState.update { it.copy(isLoading = false, isError = true) }
                }
        }
    }

    fun onAction(action: SelectCryptoPaymentMethodUiAction) {
        when (action) {
            SelectCryptoPaymentMethodUiAction.OnRetryLoadPaymentMethodsClick -> onRetryLoadPaymentMethodsClick()
            SelectCryptoPaymentMethodUiAction.OnNextClick -> onNextClick()
            is SelectCryptoPaymentMethodUiAction.OnSearchQueryChange -> onSearchQueryChange(action.query)
            is SelectCryptoPaymentMethodUiAction.OnPaymentMethodClick -> onPaymentMethodClick(action.paymentMethod)
        }
    }

    private fun onSearchQueryChange(query: String) {
        val normalizedQuery = query.trim()
        val filteredCryptoPaymentMethods = filterCryptoPaymentMethods(normalizedQuery)

        _uiState.update { currentState ->
            val selectedPaymentMethod =
                currentState.selectedPaymentMethod?.takeIf { selected -> selected in filteredCryptoPaymentMethods }

            currentState.copy(
                searchQuery = normalizedQuery,
                paymentMethods = filteredCryptoPaymentMethods,
                selectedPaymentMethod = selectedPaymentMethod,
            )
        }
    }

    private fun onPaymentMethodClick(paymentMethod: CryptoPaymentMethodVO) {
        _uiState.update { it.copy(selectedPaymentMethod = paymentMethod) }
    }

    private fun filterCryptoPaymentMethods(query: String): List<CryptoPaymentMethodVO> {
        val normalizedQuery = query.trim().lowercase()

        return allCryptoPaymentMethods.filter { method ->
            normalizedQuery.isBlank() ||
                method.name.lowercase().contains(normalizedQuery) ||
                method.code.lowercase().contains(normalizedQuery)
        }
    }

    private fun onNextClick() {
        fun showSelectionError() {
            showSnackbar("mobile.error.generic".i18n(), SnackbarType.ERROR)
        }

        val selectedPaymentMethod =
            _uiState.value.selectedPaymentMethod ?: run {
                showSelectionError()
                return
            }

        val selectedDomainPaymentMethod =
            cryptoPaymentMethodByCode[selectedPaymentMethod.code] ?: run {
                showSelectionError()
                return
            }

        presenterScope.launch {
            _effect.emit(SelectCryptoPaymentMethodEffect.NavigateToNextScreen(selectedDomainPaymentMethod))
        }
    }

    private fun onRetryLoadPaymentMethodsClick() {
        loadPaymentMethods()
    }
}
