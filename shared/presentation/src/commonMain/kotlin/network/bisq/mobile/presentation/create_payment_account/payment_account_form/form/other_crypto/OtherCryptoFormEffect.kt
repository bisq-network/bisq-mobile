package network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.other_crypto

import network.bisq.mobile.domain.model.account.crypto.OtherCryptoAssetAccount

sealed interface OtherCryptoFormEffect {
    data class NavigateToNextScreen(
        val account: OtherCryptoAssetAccount,
    ) : OtherCryptoFormEffect
}
