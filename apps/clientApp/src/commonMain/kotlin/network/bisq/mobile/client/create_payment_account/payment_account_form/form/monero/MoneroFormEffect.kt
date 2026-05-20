package network.bisq.mobile.client.create_payment_account.payment_account_form.form.monero

import network.bisq.mobile.domain.model.account.create.crypto.CreateMoneroAccount

sealed interface MoneroFormEffect {
    data class NavigateToNextScreen(
        val account: CreateMoneroAccount,
    ) : MoneroFormEffect
}
