package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.bank

import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccountType

sealed interface BankAccountFormUiAction {
    data class OnCountrySelect(
        val index: Int,
    ) : BankAccountFormUiAction

    data class OnCurrencySelect(
        val index: Int,
    ) : BankAccountFormUiAction

    data class OnHolderNameChange(
        val value: String,
    ) : BankAccountFormUiAction

    data class OnHolderIdChange(
        val value: String,
    ) : BankAccountFormUiAction

    data class OnBankNameChange(
        val value: String,
    ) : BankAccountFormUiAction

    data class OnBankIdChange(
        val value: String,
    ) : BankAccountFormUiAction

    data class OnBranchIdChange(
        val value: String,
    ) : BankAccountFormUiAction

    data class OnAccountNrChange(
        val value: String,
    ) : BankAccountFormUiAction

    data class OnBankAccountTypeSelect(
        val type: BankAccountType,
    ) : BankAccountFormUiAction

    data class OnNationalAccountIdChange(
        val value: String,
    ) : BankAccountFormUiAction
}
