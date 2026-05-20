package network.bisq.mobile.client.create_payment_account.payment_account_form.form.zelle

import network.bisq.mobile.client.create_payment_account.payment_account_form.form.AccountFormUiState
import network.bisq.mobile.presentation.common.ui.utils.DataEntry

data class ZelleFormUiState(
    val holderNameEntry: DataEntry = DataEntry(validator = ::validateHolderName),
    val emailOrMobileNrEntry: DataEntry = DataEntry(validator = ::validateEmailOrMobile),
) : AccountFormUiState
