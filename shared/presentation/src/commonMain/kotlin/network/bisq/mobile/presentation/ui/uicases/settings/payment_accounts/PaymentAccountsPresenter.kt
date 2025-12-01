package network.bisq.mobile.presentation.ui.uicases.settings.payment_accounts

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.replicated.account.UserDefinedFiatAccountPayloadVO
import network.bisq.mobile.domain.data.replicated.account.UserDefinedFiatAccountVO
import network.bisq.mobile.domain.service.accounts.AccountsServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.helpers.EMPTY_STRING
import network.bisq.mobile.presentation.ui.uicases.settings.payment_accounts.PaymentAccountsUiAction.OnAccountDescriptionChange
import network.bisq.mobile.presentation.ui.uicases.settings.payment_accounts.PaymentAccountsUiAction.OnAccountNameChange
import network.bisq.mobile.presentation.ui.uicases.settings.payment_accounts.PaymentAccountsUiAction.OnAccountSelect
import network.bisq.mobile.presentation.ui.uicases.settings.payment_accounts.PaymentAccountsUiAction.OnAddAccountClick
import network.bisq.mobile.presentation.ui.uicases.settings.payment_accounts.PaymentAccountsUiAction.OnCancelAddAccountClick
import network.bisq.mobile.presentation.ui.uicases.settings.payment_accounts.PaymentAccountsUiAction.OnCancelDeleteAccountClick
import network.bisq.mobile.presentation.ui.uicases.settings.payment_accounts.PaymentAccountsUiAction.OnCancelEditAccountClick
import network.bisq.mobile.presentation.ui.uicases.settings.payment_accounts.PaymentAccountsUiAction.OnConfirmAddAccountClick
import network.bisq.mobile.presentation.ui.uicases.settings.payment_accounts.PaymentAccountsUiAction.OnConfirmDeleteAccountClick
import network.bisq.mobile.presentation.ui.uicases.settings.payment_accounts.PaymentAccountsUiAction.OnDeleteAccountClick
import network.bisq.mobile.presentation.ui.uicases.settings.payment_accounts.PaymentAccountsUiAction.OnEditAccountClick
import network.bisq.mobile.presentation.ui.uicases.settings.payment_accounts.PaymentAccountsUiAction.OnRetryLoadAccountsClick
import network.bisq.mobile.presentation.ui.uicases.settings.payment_accounts.PaymentAccountsUiAction.OnSaveAccountClick

private const val MAX_ACCOUNT_FIELD_LENGTH = 1024
private const val MAX_ACCOUNT_NAME_FIELD_LENGTH = 256
private const val MIN_ACCOUNT_FIELD_LENGTH = 3

open class PaymentAccountsPresenter(
    private val accountsServiceFacade: AccountsServiceFacade,
    mainPresenter: MainPresenter
) : BasePresenter(mainPresenter) {

    private val _uiState = MutableStateFlow(PaymentAccountsUiState())
    val uiState: StateFlow<PaymentAccountsUiState> = _uiState.asStateFlow()

    override fun onViewAttached() {
        super.onViewAttached()
        loadAccounts()
        observeAccounts()
    }

    private fun loadAccounts() {
        presenterScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingAccounts = true,
                    isLoadingAccountsError = false
                )
            }

            val accounts = accountsServiceFacade.getAccounts()
                .onFailure { error ->
                    log.e(error) { "Failed to load accounts" }
                    _uiState.update {
                        it.copy(
                            isLoadingAccountsError = true,
                            isLoadingAccounts = false
                        )
                    }
                    return@launch
                }
                .getOrNull() ?: emptyList()

            // Only fetch selected account if there are accounts available
            if (accounts.isNotEmpty()) {
                accountsServiceFacade.getSelectedAccount()
                    .onFailure { error ->
                        log.e(error) { "Failed to load selected account" }
                        _uiState.update {
                            it.copy(
                                isLoadingAccountsError = true,
                                isLoadingAccounts = false
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
            combine(
                accountsServiceFacade.accounts,
                accountsServiceFacade.selectedAccount
            ) { accounts, selectedAccount ->
                accounts to selectedAccount
            }.collect { (accounts, selectedAccount) ->
                _uiState.update { currentState ->
                    val newSelectedAccountIndex = selectedAccount?.accountName?.let { name ->
                        accounts.indexOfFirst { it.accountName == name }
                            .takeIf { it >= 0 } ?: -1
                    } ?: -1

                    currentState.copy(
                        accounts = accounts,
                        isEditAccountMode = false,
                        selectedAccountIndex = newSelectedAccountIndex,
                        accountName = selectedAccount?.accountName ?: EMPTY_STRING,
                        accountDescription = selectedAccount?.accountPayload?.accountData
                            ?: EMPTY_STRING,
                        accountNameInvalidMessage = null,
                        accountDescriptionInvalidMessage = null
                    )
                }
            }
        }
    }

    private fun addAccount(newName: String, newDescription: String) {
        if (_uiState.value.accounts.find { it.accountName == newName } != null) {
            showSnackbar("mobile.user.paymentAccounts.createAccount.validations.name.alreadyExists".i18n())
            return
        }
        presenterScope.launch {
            showLoading()
            val newAccount = UserDefinedFiatAccountVO(
                accountName = newName,
                UserDefinedFiatAccountPayloadVO(
                    accountData = newDescription
                )
            )
            accountsServiceFacade.addAccount(newAccount)
                .onSuccess {
                    showSnackbar(
                        "mobile.user.paymentAccounts.createAccount.notifications.name.accountCreated".i18n(),
                        false
                    )
                    _uiState.update { it.copy(showAddAccountBottomSheet = false) }
                }
                .onFailure {
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

        if (selectedAccount.accountName != newName && state.accounts.find { it.accountName == newName } != null) {
            showSnackbar("mobile.user.paymentAccounts.createAccount.validations.name.alreadyExists".i18n())
            return
        }

        val accountNameInvalidMessage = validateAccountNameField(newName)
        val accountDescriptionInvalidMessage = validateAccountDescriptionField(newDescription)
        if (accountDescriptionInvalidMessage != null || accountNameInvalidMessage != null) {
            _uiState.update {
                it.copy(
                    accountNameInvalidMessage = accountNameInvalidMessage,
                    accountDescriptionInvalidMessage = accountDescriptionInvalidMessage
                )
            }
            return
        }

        presenterScope.launch {
            showLoading()
            val newAccount = UserDefinedFiatAccountVO(
                accountName = newName,
                UserDefinedFiatAccountPayloadVO(
                    accountData = newDescription
                )
            )
            accountsServiceFacade.saveAccount(newAccount)
                .onSuccess {
                    showSnackbar(
                        "mobile.user.paymentAccounts.createAccount.notifications.name.accountUpdated".i18n(),
                        false
                    )
                }
                .onFailure {
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
            accountsServiceFacade.deleteAccount(selectedAccount)
                .onSuccess {
                    showSnackbar(
                        "mobile.user.paymentAccounts.createAccount.notifications.name.accountDeleted".i18n(),
                        false
                    )
                    _uiState.update { it.copy(showDeleteConfirmationDialog = false) }
                }
                .onFailure {
                    log.e { "Couldn't remove account ${selectedAccount.accountName}" }
                    showSnackbar(
                        "mobile.user.paymentAccounts.createAccount.notifications.name.unableToDelete".i18n(
                            selectedAccount.accountName
                        ),
                        isError = true
                    )
                }
            hideLoading()
        }
    }

    private fun validateAccountNameField(name: String): String? {
        return when {
            name.length < MIN_ACCOUNT_FIELD_LENGTH ->
                "mobile.user.paymentAccounts.createAccount.validations.name.minLength".i18n()

            name.length > MAX_ACCOUNT_NAME_FIELD_LENGTH ->
                "mobile.user.paymentAccounts.createAccount.validations.name.maxLength".i18n()

            else -> null
        }
    }

    private fun validateAccountDescriptionField(description: String): String? {
        return when {
            description.length < MIN_ACCOUNT_FIELD_LENGTH ->
                "mobile.user.paymentAccounts.createAccount.validations.accountData.minLength".i18n()

            description.length > MAX_ACCOUNT_FIELD_LENGTH ->
                "mobile.user.paymentAccounts.createAccount.validations.accountData.maxLength".i18n()

            else -> null
        }
    }

    fun onAction(action: PaymentAccountsUiAction) {
        when (action) {
            is OnAccountNameChange -> onAccountNameChange(action.name)
            is OnAccountDescriptionChange -> onAccountDescriptionChange(
                action.description
            )

            is OnAddAccountClick -> onAddAccountClick()
            is OnCancelAddAccountClick -> onCancelAddAccountClick()
            is OnConfirmAddAccountClick -> onConfirmAddAccountClick(
                action.name,
                action.description
            )

            is OnDeleteAccountClick -> onDeleteAccountClick()
            is OnCancelDeleteAccountClick -> onCancelDeleteAccountClick()
            is OnConfirmDeleteAccountClick -> onConfirmDeleteAccountClick()
            is OnSaveAccountClick -> onSaveAccountClick()
            is OnRetryLoadAccountsClick -> onRetryLoadAccountsClick()
            is OnAccountSelect -> onAccountSelect(action.index)
            is OnEditAccountClick -> onEditAccountClick()
            is OnCancelEditAccountClick -> onCancelEditAccountClick()
        }
    }


    private fun onRetryLoadAccountsClick() {
        loadAccounts()
    }

    private fun onAddAccountClick() {
        _uiState.update { it.copy(showAddAccountBottomSheet = true) }
    }

    private fun onEditAccountClick() {
        _uiState.update { it.copy(isEditAccountMode = true) }
    }

    private fun onSaveAccountClick() {
        saveAccount()
    }

    private fun onDeleteAccountClick() {
        _uiState.update { it.copy(showDeleteConfirmationDialog = true) }
    }

    private fun onAccountSelect(index: Int) {
        if (_uiState.value.selectedAccountIndex == index) return
        val account = _uiState.value.accounts.getOrNull(index) ?: return
        presenterScope.launch {
            showLoading()
            accountsServiceFacade.setSelectedAccount(account)
                .onFailure { error ->
                    log.e(error) { "Failed to select account at index $index" }
                    showSnackbar("mobile.error.generic".i18n(), true)
                }
            hideLoading()
        }
    }

    private fun onAccountNameChange(name: String) {
        _uiState.update { it.copy(accountName = name, accountNameInvalidMessage = null) }
    }

    private fun onAccountDescriptionChange(description: String) {
        _uiState.update {
            it.copy(
                accountDescription = description,
                accountDescriptionInvalidMessage = null
            )
        }
    }

    private fun onCancelEditAccountClick() {
        _uiState.update {
            it.copy(
                isEditAccountMode = false,
                accountNameInvalidMessage = null,
                accountDescriptionInvalidMessage = null
            )
        }
    }

    private fun onCancelAddAccountClick() {
        _uiState.update { it.copy(showAddAccountBottomSheet = false) }
    }

    private fun onConfirmAddAccountClick(name: String, description: String) {
        addAccount(name, description)
    }

    private fun onCancelDeleteAccountClick() {
        _uiState.update { it.copy(showDeleteConfirmationDialog = false) }
    }

    private fun onConfirmDeleteAccountClick() {
        deleteSelectedAccount()
    }
}
