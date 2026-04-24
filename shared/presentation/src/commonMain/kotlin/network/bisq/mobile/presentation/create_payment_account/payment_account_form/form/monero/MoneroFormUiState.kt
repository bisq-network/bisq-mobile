package network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.monero

import network.bisq.mobile.presentation.common.ui.utils.DataEntry
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.AccountFormUiState
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.crypto.CryptoAccountFormUiState

data class MoneroFormUiState(
    val crypto: CryptoAccountFormUiState = CryptoAccountFormUiState(),
    val useSubAddresses: Boolean = false,
    val mainAddressEntry: DataEntry = DataEntry(validator = ::validateMainAddress),
    val privateViewKeyEntry: DataEntry = DataEntry(validator = ::validatePrivateViewKey),
    val accountIndexEntry: DataEntry = DataEntry(validator = ::validateAccountIndex),
    val initialSubAddressIndexEntry: DataEntry = DataEntry(validator = ::validateInitialSubAddressIndex),
    val subAddressEntry: DataEntry = DataEntry(value = SUB_ADDRESS_PLACEHOLDER),
) : AccountFormUiState
