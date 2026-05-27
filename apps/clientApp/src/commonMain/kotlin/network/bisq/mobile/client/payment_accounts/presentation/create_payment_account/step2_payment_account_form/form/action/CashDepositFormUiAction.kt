package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.action

import network.bisq.mobile.domain.model.account.fiat.BankAccountType

sealed interface CashDepositFormUiAction : AccountFormUiAction {
    data class OnCountrySelect(
        val index: Int,
    ) : CashDepositFormUiAction

    data class OnCurrencySelect(
        val index: Int,
    ) : CashDepositFormUiAction

    data class OnHolderNameChange(
        val value: String,
    ) : CashDepositFormUiAction

    data class OnHolderIdChange(
        val value: String,
    ) : CashDepositFormUiAction

    data class OnBankNameChange(
        val value: String,
    ) : CashDepositFormUiAction

    data class OnBankIdChange(
        val value: String,
    ) : CashDepositFormUiAction

    data class OnBranchIdChange(
        val value: String,
    ) : CashDepositFormUiAction

    data class OnAccountNrChange(
        val value: String,
    ) : CashDepositFormUiAction

    data class OnBankAccountTypeSelect(
        val type: BankAccountType,
    ) : CashDepositFormUiAction

    data class OnNationalAccountIdChange(
        val value: String,
    ) : CashDepositFormUiAction

    data class OnRequirementsChange(
        val value: String,
    ) : CashDepositFormUiAction
}
