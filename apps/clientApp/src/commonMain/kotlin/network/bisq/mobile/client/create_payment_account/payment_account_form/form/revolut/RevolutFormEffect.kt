package network.bisq.mobile.client.create_payment_account.payment_account_form.form.revolut

import network.bisq.mobile.domain.model.account.create.fiat.CreateRevolutAccount

sealed interface RevolutFormEffect {
    data class NavigateToNextScreen(
        val account: CreateRevolutAccount,
    ) : RevolutFormEffect
}
