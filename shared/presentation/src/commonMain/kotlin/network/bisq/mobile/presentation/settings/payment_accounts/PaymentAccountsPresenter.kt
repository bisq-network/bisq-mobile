package network.bisq.mobile.presentation.settings.payment_accounts

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.replicated.account.fiat.UserDefinedFiatAccountPayloadVO
import network.bisq.mobile.domain.data.replicated.account.fiat.UserDefinedFiatAccountVO
import network.bisq.mobile.domain.service.accounts.FiatAccountsServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING
import network.bisq.mobile.presentation.main.MainPresenter

private const val MAX_ACCOUNT_FIELD_LENGTH = 1000
private const val MAX_ACCOUNT_NAME_FIELD_LENGTH = 256
private const val MIN_ACCOUNT_FIELD_LENGTH = 3

open class PaymentAccountsPresenter(
    private val fiatAccountsServiceFacade: FiatAccountsServiceFacade,
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter) {
    private val _uiState = MutableStateFlow(PaymentAccountsUiState())
    val uiState: StateFlow<PaymentAccountsUiState> = _uiState.asStateFlow()

    override fun onViewAttached() {
        super.onViewAttached()
        loadAccounts()
        observeAccounts()
    }

    fun onAction(action: PaymentAccountsUiAction) {
        when (action) {
            is PaymentAccountsUiAction.OnAccountNameChange -> onAccountNameChange(action.name)
            is PaymentAccountsUiAction.OnAccountDescriptionChange ->
                onAccountDescriptionChange(
                    action.description,
                )

            is PaymentAccountsUiAction.OnAddAccountClick -> onAddAccountClick()
            is PaymentAccountsUiAction.OnConfirmAddAccountClick -> onConfirmAddAccountClick()
            is PaymentAccountsUiAction.OnDeleteAccountClick -> onDeleteAccountClick()
            is PaymentAccountsUiAction.OnCancelDeleteAccountClick -> onCancelDeleteAccountClick()
            is PaymentAccountsUiAction.OnConfirmDeleteAccountClick -> onConfirmDeleteAccountClick()
            is PaymentAccountsUiAction.OnSaveAccountClick -> onSaveAccountClick()
            is PaymentAccountsUiAction.OnRetryLoadAccountsClick -> onRetryLoadAccountsClick()
            is PaymentAccountsUiAction.OnAccountSelect -> onAccountSelect(action.index)
            is PaymentAccountsUiAction.OnEditAccountClick -> onEditAccountClick()
            is PaymentAccountsUiAction.OnCancelAddEditAccountClick -> onCancelAddEditAccountClick()
        }
    }

    private fun loadAccounts() {
        presenterScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingAccounts = true,
                    isLoadingAccountsError = false,
                )
            }

            val accounts =
                fiatAccountsServiceFacade
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
                    }.getOrNull() ?: emptyList()

            // Only fetch selected account if there are accounts available
            if (accounts.isNotEmpty()) {
                fiatAccountsServiceFacade
                    .getSelectedAccount()
                    .onFailure { error ->
                        log.e(error) { "Failed to load selected account" }
                        _uiState.update {
                            it.copy(
                                isLoadingAccountsError = true,
                                isLoadingAccounts = false,
                            )
                        }
                        return@launch
                    }
            }

            _uiState.update { it.copy(isLoadingAccounts = false) }
        }
    }

    private fun observeAccounts() {
        presenterScope.launch {
            fiatAccountsServiceFacade.accountState.collect { state ->
                val accounts = state.accounts.filterIsInstance<UserDefinedFiatAccountVO>()
                val selectedAccount = accounts.getOrNull(state.selectedAccountIndex)
                _uiState.update {
                    it.copy(
                        accounts = accounts,
                        selectedAccountIndex = state.selectedAccountIndex,
                        showEditAccountState = false,
                        showAddAccountState = false,
                        accountName = selectedAccount?.accountName ?: EMPTY_STRING,
                        accountDescription =
                            selectedAccount?.accountPayload?.accountData
                                ?: EMPTY_STRING,
                        accountNameInvalidMessage = null,
                        accountDescriptionInvalidMessage = null,
                    )
                }
            }
        }
    }

    private fun validateAccountFields(
        name: String,
        description: String,
        excludeAccountName: String? = null, // When editing, exclude the current account name from duplicate check
    ): Boolean {
        // Check for duplicate account name
        val isDuplicate =
            _uiState.value.accounts.any {
                it.accountName == name && it.accountName != excludeAccountName
            }
        if (isDuplicate) {
            showSnackbar("mobile.user.paymentAccounts.createAccount.validations.name.alreadyExists".i18n())
            return false
        }

        // Validate fields
        val accountNameInvalidMessage = validateAccountNameField(name)
        val accountDescriptionInvalidMessage = validateAccountDescriptionField(description)

        if (accountDescriptionInvalidMessage != null || accountNameInvalidMessage != null) {
            _uiState.update {
                it.copy(
                    accountNameInvalidMessage = accountNameInvalidMessage,
                    accountDescriptionInvalidMessage = accountDescriptionInvalidMessage,
                )
            }
            return false
        }

        return true
    }

    private fun createAccountVO(
        name: String,
        description: String,
    ): UserDefinedFiatAccountVO =
        UserDefinedFiatAccountVO(
            accountName = name,
            accountPayload =
                UserDefinedFiatAccountPayloadVO(
                    accountData = description,
                ),
        )

    private fun addAccount() {
        val newName = _uiState.value.accountName
        val newDescription = _uiState.value.accountDescription

        if (!validateAccountFields(newName, newDescription)) {
            return
        }

        presenterScope.launch {
            showLoading()
            val newAccount = createAccountVO(newName, newDescription)
            fiatAccountsServiceFacade
                .addAccount(newAccount)
                .onSuccess {
                    showSnackbar(
                        "mobile.user.paymentAccounts.createAccount.notifications.name.accountCreated".i18n(),
                        false,
                    )
                    _uiState.update { it.copy(showAddAccountState = false) }
                }.onFailure {
                    showSnackbar("mobile.error.generic".i18n(), true)
                }
            hideLoading()
        }
    }

    private fun saveAccount() {
        val state = _uiState.value
        val newName = state.accountName
        val newDescription = state.accountDescription
        val selectedAccount = state.accounts.getOrNull(state.selectedAccountIndex)
        if (selectedAccount == null) return

        if (!validateAccountFields(newName, newDescription, selectedAccount.accountName)) {
            return
        }

        presenterScope.launch {
            showLoading()
            val newAccount = createAccountVO(newName, newDescription)
            fiatAccountsServiceFacade
                .saveAccount(newAccount)
                .onSuccess {
                    showSnackbar(
                        "mobile.user.paymentAccounts.createAccount.notifications.name.accountUpdated".i18n(),
                        false,
                    )
                }.onFailure {
                    showSnackbar("mobile.error.generic".i18n(), true)
                }
            hideLoading()
        }
    }

    private fun deleteSelectedAccount() {
        val state = _uiState.value
        val selectedAccount = state.accounts.getOrNull(state.selectedAccountIndex)
        if (selectedAccount == null) return
        presenterScope.launch {
            showLoading()
            fiatAccountsServiceFacade
                .deleteAccount(selectedAccount)
                .onSuccess {
                    showSnackbar(
                        "mobile.user.paymentAccounts.createAccount.notifications.name.accountDeleted".i18n(),
                        false,
                    )
                    _uiState.update { it.copy(showDeleteConfirmationDialog = false) }
                }.onFailure {
                    log.e { "Couldn't remove account ${selectedAccount.accountName}" }
                    showSnackbar(
                        "mobile.user.paymentAccounts.createAccount.notifications.name.unableToDelete".i18n(
                            selectedAccount.accountName,
                        ),
                        isError = true,
                    )
                }
            hideLoading()
        }
    }

    private fun validateAccountNameField(name: String): String? =
        when {
            name.length < MIN_ACCOUNT_FIELD_LENGTH ->
                "mobile.user.paymentAccounts.createAccount.validations.name.minLength".i18n()

            name.length > MAX_ACCOUNT_NAME_FIELD_LENGTH ->
                "mobile.user.paymentAccounts.createAccount.validations.name.maxLength".i18n()

            else -> null
        }

    private fun validateAccountDescriptionField(description: String): String? =
        when {
            description.length < MIN_ACCOUNT_FIELD_LENGTH ->
                "mobile.user.paymentAccounts.createAccount.validations.accountData.minLength".i18n()

            description.length > MAX_ACCOUNT_FIELD_LENGTH ->
                "mobile.user.paymentAccounts.createAccount.validations.accountData.maxLength".i18n()

            else -> null
        }

    private fun onRetryLoadAccountsClick() {
        loadAccounts()
    }

    private fun onAddAccountClick() {
        _uiState.update {
            it.copy(
                showAddAccountState = true,
                accountNameInvalidMessage = null,
                accountDescriptionInvalidMessage = null,
                accountName = EMPTY_STRING,
                accountDescription = EMPTY_STRING,
            )
        }
    }

    private fun onEditAccountClick() {
        _uiState.update {
            it.copy(showEditAccountState = true)
        }
    }

    private fun onSaveAccountClick() {
        saveAccount()
    }

    private fun onDeleteAccountClick() {
        _uiState.update { it.copy(showDeleteConfirmationDialog = true) }
    }

    private fun onAccountSelect(index: Int) {
        if (_uiState.value.selectedAccountIndex == index) return
        presenterScope.launch {
            fiatAccountsServiceFacade.setSelectedAccountIndex(index)
        }
    }

    private fun onAccountNameChange(name: String) {
        _uiState.update { it.copy(accountName = name, accountNameInvalidMessage = null) }
    }

    private fun onAccountDescriptionChange(description: String) {
        _uiState.update {
            it.copy(
                accountDescription = description,
                accountDescriptionInvalidMessage = null,
            )
        }
    }

    private fun onCancelAddEditAccountClick() {
        _uiState.update {
            val selectedAccount = it.accounts.getOrNull(it.selectedAccountIndex)
            it.copy(
                showEditAccountState = false,
                showAddAccountState = false,
                accountNameInvalidMessage = null,
                accountDescriptionInvalidMessage = null,
                accountName = selectedAccount?.accountName ?: EMPTY_STRING,
                accountDescription =
                    selectedAccount?.accountPayload?.accountData
                        ?: EMPTY_STRING,
            )
        }
    }

    private fun onConfirmAddAccountClick() {
        addAccount()
    }

    private fun onCancelDeleteAccountClick() {
        _uiState.update { it.copy(showDeleteConfirmationDialog = false) }
    }

    private fun onConfirmDeleteAccountClick() {
        deleteSelectedAccount()
    }
}
