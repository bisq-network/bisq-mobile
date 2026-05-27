package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step1_select_payment_method.fiat

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bisq.mobile.client.common.presentation.model.account.FiatPaymentMethodChargebackRiskVO
import network.bisq.mobile.client.common.presentation.model.account.FiatPaymentMethodVO
import network.bisq.mobile.client.common.presentation.model.account.toVO
import network.bisq.mobile.data.service.accounts.PaymentAccountsServiceFacade
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethod
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType
import network.bisq.mobile.presentation.main.MainPresenter

class SelectFiatPaymentMethodPresenter(
    private val paymentAccountsServiceFacade: PaymentAccountsServiceFacade,
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter) {
    private val _uiState =
        MutableStateFlow(
            SelectFiatPaymentMethodUiState(),
        )
    val uiState: StateFlow<SelectFiatPaymentMethodUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<SelectFiatPaymentMethodEffect>()
    val effect = _effect.asSharedFlow()

    private var allFiatPaymentMethods: List<FiatPaymentMethodVO> = emptyList()
    private var fiatPaymentMethodByPaymentType: Map<String, FiatPaymentMethod> = emptyMap()

    override fun onViewAttached() {
        super.onViewAttached()
        loadPaymentMethods()
    }

    fun loadPaymentMethods() {
        presenterScope.launch {
            _uiState.update { it.copy(isLoading = true, isError = false) }
            paymentAccountsServiceFacade
                .getFiatPaymentMethods()
                .onSuccess { paymentMethods ->
                    fiatPaymentMethodByPaymentType = paymentMethods.associateBy { it.paymentRail.name }
                    allFiatPaymentMethods = paymentMethods.mapNotNull { it.toVO() }
                    val query = _uiState.value.searchQuery
                    val riskFilter = _uiState.value.activeRiskFilter
                    val filteredFiatPaymentMethods = filterFiatPaymentMethods(query, riskFilter)
                    _uiState.update {
                        it.copy(
                            paymentMethods = filteredFiatPaymentMethods,
                            isLoading = false,
                        )
                    }
                }.onFailure {
                    _uiState.update { it.copy(isLoading = false, isError = true) }
                }
        }
    }

    fun onAction(action: SelectFiatPaymentMethodUiAction) {
        when (action) {
            SelectFiatPaymentMethodUiAction.OnRetryLoadPaymentMethodsClick -> onRetryLoadPaymentMethodsClick()
            SelectFiatPaymentMethodUiAction.OnNextClick -> onNextClick()
            is SelectFiatPaymentMethodUiAction.OnSearchQueryChange -> onSearchQueryChange(action.query)
            is SelectFiatPaymentMethodUiAction.OnRiskFilterChange -> onRiskFilterChange(action.riskFilter)
            is SelectFiatPaymentMethodUiAction.OnPaymentMethodClick -> onPaymentMethodClick(action.paymentMethod)
        }
    }

    private fun onSearchQueryChange(query: String) {
        val normalizedQuery = query.trim()
        val riskFilter = _uiState.value.activeRiskFilter
        val filteredFiatPaymentMethods = filterFiatPaymentMethods(normalizedQuery, riskFilter)

        _uiState.update { currentState ->
            val selectedPaymentMethod =
                currentState.selectedPaymentMethod?.takeIf { selected -> selected in filteredFiatPaymentMethods }

            currentState.copy(
                searchQuery = normalizedQuery,
                paymentMethods = filteredFiatPaymentMethods,
                selectedPaymentMethod = selectedPaymentMethod,
            )
        }
    }

    private fun onRiskFilterChange(riskFilter: FiatPaymentMethodChargebackRiskVO?) {
        val query = _uiState.value.searchQuery
        val filteredFiatPaymentMethods = filterFiatPaymentMethods(query, riskFilter)

        _uiState.update { currentState ->
            val selectedPaymentMethod =
                currentState.selectedPaymentMethod?.takeIf { selected -> selected in filteredFiatPaymentMethods }

            currentState.copy(
                activeRiskFilter = riskFilter,
                paymentMethods = filteredFiatPaymentMethods,
                selectedPaymentMethod = selectedPaymentMethod,
            )
        }
    }

    private fun onPaymentMethodClick(paymentMethod: FiatPaymentMethodVO) {
        _uiState.update {
            it.copy(selectedPaymentMethod = paymentMethod)
        }
    }

    private fun filterFiatPaymentMethods(
        query: String,
        riskFilter: FiatPaymentMethodChargebackRiskVO?,
    ): List<FiatPaymentMethodVO> {
        val normalizedQuery = query.trim().lowercase()

        return allFiatPaymentMethods.filter { method ->
            val matchesSearch = normalizedQuery.isBlank() || method.name.lowercase().contains(normalizedQuery)
            val matchesRisk = riskFilter == null || method.chargebackRisk == riskFilter
            matchesSearch && matchesRisk
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
            fiatPaymentMethodByPaymentType[selectedPaymentMethod.paymentType.name] ?: run {
                showSelectionError()
                return
            }

        presenterScope.launch {
            _effect.emit(SelectFiatPaymentMethodEffect.NavigateToNextScreen(selectedDomainPaymentMethod))
        }
    }

    private fun onRetryLoadPaymentMethodsClick() {
        loadPaymentMethods()
    }
}
