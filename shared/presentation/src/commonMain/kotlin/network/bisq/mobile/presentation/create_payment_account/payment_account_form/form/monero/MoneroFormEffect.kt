package network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.monero

import network.bisq.mobile.domain.model.account.crypto.MoneroAccount

sealed interface MoneroFormEffect {
    data class NavigateToNextScreen(
        val account: MoneroAccount,
    ) : MoneroFormEffect
}
