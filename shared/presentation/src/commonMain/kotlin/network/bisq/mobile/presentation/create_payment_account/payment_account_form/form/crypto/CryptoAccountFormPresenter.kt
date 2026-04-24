package network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.crypto

import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.AccountFormPresenter
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.action.AccountFormUiAction
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.action.CryptoAccountFormUiAction
import network.bisq.mobile.presentation.main.MainPresenter

abstract class CryptoAccountFormPresenter(
    mainPresenter: MainPresenter,
) : AccountFormPresenter(mainPresenter) {
    protected abstract fun updateCryptoUiState(transform: (CryptoAccountFormUiState) -> CryptoAccountFormUiState)

    override fun onCustomAction(action: AccountFormUiAction) {
        if (!handleCryptoAction(action)) {
            onCryptoCustomAction(action)
        }
    }

    protected open fun onCryptoCustomAction(action: AccountFormUiAction) = Unit

    private fun handleCryptoAction(action: AccountFormUiAction): Boolean =
        when (action) {
            is CryptoAccountFormUiAction.OnAddressChange -> {
                updateCryptoUiState {
                    it.copy(
                        addressEntry = it.addressEntry.updateValue(action.value),
                    )
                }
                true
            }

            is CryptoAccountFormUiAction.OnIsInstantChange -> {
                updateCryptoUiState { it.copy(isInstant = action.value) }
                true
            }

            is CryptoAccountFormUiAction.OnIsAutoConfChange -> {
                updateCryptoUiState { it.copy(isAutoConf = action.value) }
                true
            }

            is CryptoAccountFormUiAction.OnAutoConfNumConfirmationsChange -> {
                updateCryptoUiState {
                    it.copy(
                        autoConfNumConfirmationsEntry =
                            it.autoConfNumConfirmationsEntry.updateValue(action.value),
                    )
                }
                true
            }

            is CryptoAccountFormUiAction.OnAutoConfMaxTradeAmountChange -> {
                updateCryptoUiState {
                    it.copy(
                        autoConfMaxTradeAmountEntry =
                            it.autoConfMaxTradeAmountEntry.updateValue(action.value),
                    )
                }
                true
            }

            is CryptoAccountFormUiAction.OnAutoConfExplorerUrlsChange -> {
                updateCryptoUiState {
                    it.copy(
                        autoConfExplorerUrlsEntry =
                            it.autoConfExplorerUrlsEntry.updateValue(action.value),
                    )
                }
                true
            }

            else -> false
        }

    protected fun validateCryptoAutoConfState(current: CryptoAccountFormUiState): CryptoAccountFormUiState =
        current.copy(
            autoConfNumConfirmationsEntry =
                if (current.isAutoConf) {
                    current.autoConfNumConfirmationsEntry.validate()
                } else {
                    current.autoConfNumConfirmationsEntry.clearError()
                },
            autoConfMaxTradeAmountEntry =
                if (current.isAutoConf) {
                    current.autoConfMaxTradeAmountEntry.validate()
                } else {
                    current.autoConfMaxTradeAmountEntry.clearError()
                },
            autoConfExplorerUrlsEntry =
                if (current.isAutoConf) {
                    current.autoConfExplorerUrlsEntry.validate()
                } else {
                    current.autoConfExplorerUrlsEntry.clearError()
                },
        )

    protected fun isCryptoAutoConfStateValid(current: CryptoAccountFormUiState): Boolean =
        current.autoConfNumConfirmationsEntry.isValid &&
            current.autoConfMaxTradeAmountEntry.isValid &&
            current.autoConfExplorerUrlsEntry.isValid
}
