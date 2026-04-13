package network.bisq.mobile.presentation.create_payment_account.select_payment_method

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bisq.mobile.data.service.accounts.PaymentAccountsServiceFacade
import network.bisq.mobile.presentation.common.model.account.FiatPaymentMethodChargebackRiskVO
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.navigation.types.PaymentAccountType
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.CryptoPaymentMethodVO
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.FiatPaymentMethodVO
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.toVO
import network.bisq.mobile.presentation.main.MainPresenter

open class SelectPaymentMethodPresenter(
    private val paymentAccountsServiceFacade: PaymentAccountsServiceFacade,
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter) {
    private val _uiState =
        MutableStateFlow(
            SelectPaymentMethodUiState(),
        )
    val uiState: StateFlow<SelectPaymentMethodUiState> = _uiState.asStateFlow()

    private var allFiatPaymentMethods: List<FiatPaymentMethodVO> = emptyList()
    private var allCryptoPaymentMethods: List<CryptoPaymentMethodVO> = emptyList()

    var accountType: PaymentAccountType? = null

    fun initialize(type: PaymentAccountType) {
        accountType = type
        loadPaymentMethods()
    }

    fun loadPaymentMethods() {
        val type = accountType ?: return
        presenterScope.launch {
            _uiState.update { it.copy(isLoading = true, isError = false) }
            when (type) {
                PaymentAccountType.FIAT -> {
                    paymentAccountsServiceFacade
                        .getFiatPaymentMethods()
                        .onSuccess { paymentMethods ->
                            allFiatPaymentMethods = paymentMethods.mapNotNull { it.toVO() }
                            val query = _uiState.value.searchQuery
                            val riskFilter = _uiState.value.activeRiskFilter
                            val filteredFiatPaymentMethods = filterFiatPaymentMethods(query, riskFilter)
                            _uiState.update {
                                it.copy(
                                    fiatPaymentMethods = filteredFiatPaymentMethods,
                                    isLoading = false,
                                )
                            }
                        }.onFailure {
                            _uiState.update { it.copy(isLoading = false, isError = true) }
                        }
                }

                PaymentAccountType.CRYPTO -> {
                    paymentAccountsServiceFacade
                        .getCryptoPaymentMethods()
                        .onSuccess { cryptoPaymentMethods ->
                            allCryptoPaymentMethods = cryptoPaymentMethods.mapNotNull { it.toVO() }
                            val query = _uiState.value.searchQuery
                            val filteredCryptoPaymentMethods = filterCryptoPaymentMethods(query)
                            _uiState.update {
                                it.copy(
                                    cryptoPaymentMethods = filteredCryptoPaymentMethods,
                                    isLoading = false,
                                )
                            }
                        }.onFailure {
                            _uiState.update { it.copy(isLoading = false, isError = true) }
                        }
                }
            }
        }
    }

    fun onAction(action: SelectPaymentMethodUiAction) {
        when (action) {
            SelectPaymentMethodUiAction.OnRetryLoadPaymentMethodsClick -> onRetryLoadPaymentMethodsClick()
            is SelectPaymentMethodUiAction.OnSearchQueryChange -> onSearchQueryChange(action.query)
            is SelectPaymentMethodUiAction.OnRiskFilterChange -> onRiskFilterChange(action.riskFilter)
            is SelectPaymentMethodUiAction.OnFiatPaymentMethodClick -> onFiatPaymentMethodClick(action.paymentMethod)
            is SelectPaymentMethodUiAction.OnCryptoPaymentMethodClick -> onCryptoPaymentMethodClick(action.paymentMethod)
        }
    }

    private fun onSearchQueryChange(query: String) {
        val normalizedQuery = query.trim()
        val riskFilter = _uiState.value.activeRiskFilter
        val filteredFiatPaymentMethods = filterFiatPaymentMethods(normalizedQuery, riskFilter)
        val filteredCryptoPaymentMethods = filterCryptoPaymentMethods(normalizedQuery)

        _uiState.update { currentState ->
            val selectedFiatPaymentMethod =
                currentState.selectedFiatPaymentMethod?.takeIf { selected -> selected in filteredFiatPaymentMethods }
            val selectedCryptoPaymentMethod =
                currentState.selectedCryptoPaymentMethod?.takeIf { selected -> selected in filteredCryptoPaymentMethods }

            currentState.copy(
                searchQuery = normalizedQuery,
                fiatPaymentMethods = filteredFiatPaymentMethods,
                cryptoPaymentMethods = filteredCryptoPaymentMethods,
                selectedFiatPaymentMethod = selectedFiatPaymentMethod,
                selectedCryptoPaymentMethod = selectedCryptoPaymentMethod,
            )
        }
    }

    private fun onRiskFilterChange(riskFilter: FiatPaymentMethodChargebackRiskVO?) {
        val query = _uiState.value.searchQuery
        val filteredFiatPaymentMethods = filterFiatPaymentMethods(query, riskFilter)
        val filteredCryptoPaymentMethods = filterCryptoPaymentMethods(query)

        _uiState.update { currentState ->
            val selectedFiatPaymentMethod =
                currentState.selectedFiatPaymentMethod?.takeIf { selected -> selected in filteredFiatPaymentMethods }
            val selectedCryptoPaymentMethod =
                currentState.selectedCryptoPaymentMethod?.takeIf { selected -> selected in filteredCryptoPaymentMethods }

            currentState.copy(
                activeRiskFilter = riskFilter,
                fiatPaymentMethods = filteredFiatPaymentMethods,
                cryptoPaymentMethods = filteredCryptoPaymentMethods,
                selectedFiatPaymentMethod = selectedFiatPaymentMethod,
                selectedCryptoPaymentMethod = selectedCryptoPaymentMethod,
            )
        }
    }

    private fun onFiatPaymentMethodClick(paymentMethod: FiatPaymentMethodVO) {
        _uiState.update {
            it.copy(
                selectedFiatPaymentMethod = paymentMethod,
                selectedCryptoPaymentMethod = null,
            )
        }
    }

    private fun onCryptoPaymentMethodClick(paymentMethod: CryptoPaymentMethodVO) {
        _uiState.update {
            it.copy(
                selectedCryptoPaymentMethod = paymentMethod,
                selectedFiatPaymentMethod = null,
            )
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

    private fun filterCryptoPaymentMethods(query: String): List<CryptoPaymentMethodVO> {
        val normalizedQuery = query.trim().lowercase()

        return allCryptoPaymentMethods.filter { method ->
            normalizedQuery.isBlank() ||
                method.name.lowercase().contains(normalizedQuery) ||
                method.code.lowercase().contains(normalizedQuery)
        }
    }

    private fun onRetryLoadPaymentMethodsClick() {
        loadPaymentMethods()
    }
}
