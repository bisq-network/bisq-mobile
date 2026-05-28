package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.ach_transfer

import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccountType
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.AccountFormUiState
import network.bisq.mobile.presentation.common.ui.utils.DataEntry

data class AchTransferFormUiState(
    val holderNameEntry: DataEntry = DataEntry(validator = ::validateHolderName),
    val holderAddressEntry: DataEntry = DataEntry(validator = ::validateHolderAddress),
    val bankNameEntry: DataEntry = DataEntry(validator = ::validateBankName),
    val routingNrEntry: DataEntry = DataEntry(validator = ::validateRoutingNr),
    val accountNrEntry: DataEntry = DataEntry(validator = ::validateAccountNr),
    val selectedBankAccountType: BankAccountType? = null,
    val bankAccountTypeErrorMessage: String? = null,
) : AccountFormUiState
