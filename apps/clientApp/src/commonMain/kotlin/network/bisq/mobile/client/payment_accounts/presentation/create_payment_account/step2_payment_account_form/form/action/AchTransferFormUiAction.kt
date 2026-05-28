package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.action

import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccountType

sealed interface AchTransferFormUiAction : AccountFormUiAction {
    data class OnHolderNameChange(
        val value: String,
    ) : AchTransferFormUiAction

    data class OnHolderAddressChange(
        val value: String,
    ) : AchTransferFormUiAction

    data class OnBankNameChange(
        val value: String,
    ) : AchTransferFormUiAction

    data class OnRoutingNrChange(
        val value: String,
    ) : AchTransferFormUiAction

    data class OnAccountNrChange(
        val value: String,
    ) : AchTransferFormUiAction

    data class OnBankAccountTypeSelect(
        val type: BankAccountType,
    ) : AchTransferFormUiAction
}
