package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.other_crypto

import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.AccountFormUiState
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.crypto.CryptoAccountFormUiState

data class OtherCryptoFormUiState(
    val crypto: CryptoAccountFormUiState = CryptoAccountFormUiState(),
) : AccountFormUiState
