package network.bisq.mobile.client.create_payment_account.payment_account_form.form.other_crypto

import network.bisq.mobile.client.create_payment_account.payment_account_form.form.AccountFormUiState
import network.bisq.mobile.client.create_payment_account.payment_account_form.form.crypto.CryptoAccountFormUiState

data class OtherCryptoFormUiState(
    val crypto: CryptoAccountFormUiState = CryptoAccountFormUiState(),
) : AccountFormUiState
