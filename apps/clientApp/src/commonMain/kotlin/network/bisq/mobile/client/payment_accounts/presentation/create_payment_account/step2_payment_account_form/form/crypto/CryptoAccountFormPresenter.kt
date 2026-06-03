package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.crypto

import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.AccountFormPresenter
import network.bisq.mobile.presentation.main.MainPresenter

abstract class CryptoAccountFormPresenter(
    mainPresenter: MainPresenter,
) : AccountFormPresenter(mainPresenter) {
    protected abstract fun updateCryptoUiState(transform: (CryptoAccountFormUiState) -> CryptoAccountFormUiState)

    fun onCryptoCommonAction(action: CryptoAccountFormUiAction) {
        when (action) {
            is CryptoAccountFormUiAction.OnAddressChange -> {
                updateCryptoUiState {
                    it.copy(
                        addressEntry = it.addressEntry.updateValue(action.value),
                    )
                }
            }

            is CryptoAccountFormUiAction.OnIsInstantChange -> {
                updateCryptoUiState { it.copy(isInstant = action.value) }
            }

            is CryptoAccountFormUiAction.OnIsAutoConfChange -> {
                updateCryptoUiState { it.copy(isAutoConf = action.value) }
            }

            is CryptoAccountFormUiAction.OnAutoConfNumConfirmationsChange -> {
                updateCryptoUiState {
                    it.copy(
                        autoConfNumConfirmationsEntry =
                            it.autoConfNumConfirmationsEntry.updateValue(action.value),
                    )
                }
            }

            is CryptoAccountFormUiAction.OnAutoConfMaxTradeAmountChange -> {
                updateCryptoUiState {
                    it.copy(
                        autoConfMaxTradeAmountEntry =
                            it.autoConfMaxTradeAmountEntry.updateValue(action.value),
                    )
                }
            }

            is CryptoAccountFormUiAction.OnAutoConfExplorerUrlsChange -> {
                updateCryptoUiState {
                    it.copy(
                        autoConfExplorerUrlsEntry =
                            it.autoConfExplorerUrlsEntry.updateValue(action.value),
                    )
                }
            }
        }
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
