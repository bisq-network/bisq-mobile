package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.zelle

import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.AccountFormUiState
import network.bisq.mobile.presentation.common.ui.utils.DataEntry

data class ZelleFormUiState(
    val holderNameEntry: DataEntry = DataEntry(validator = ::validateHolderName),
    val emailOrMobileNrEntry: DataEntry = DataEntry(validator = ::validateEmailOrMobile),
) : AccountFormUiState
