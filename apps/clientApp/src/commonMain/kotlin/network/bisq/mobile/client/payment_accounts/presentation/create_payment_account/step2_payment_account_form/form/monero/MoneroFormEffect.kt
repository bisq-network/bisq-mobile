package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.monero

import network.bisq.mobile.client.payment_accounts.domain.model.crypto.monero.CreateMoneroAccount

sealed interface MoneroFormEffect {
    data class NavigateToNextScreen(
        val account: CreateMoneroAccount,
    ) : MoneroFormEffect
}
