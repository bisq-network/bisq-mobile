package network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.zelle

import network.bisq.mobile.presentation.common.ui.utils.DataEntry
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.AccountFormUiState

data class ZelleFormUiState(
    val holderNameEntry: DataEntry = DataEntry(validator = ::validateHolderName),
    val emailOrMobileNrEntry: DataEntry = DataEntry(validator = ::validateEmailOrMobile),
) : AccountFormUiState
