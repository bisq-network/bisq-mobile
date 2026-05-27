package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.other_crypto

import network.bisq.mobile.client.payment_accounts.domain.model.crypto.other_crypto.CreateOtherCryptoAssetAccount

sealed interface OtherCryptoFormEffect {
    data class NavigateToNextScreen(
        val account: CreateOtherCryptoAssetAccount,
    ) : OtherCryptoFormEffect
}
