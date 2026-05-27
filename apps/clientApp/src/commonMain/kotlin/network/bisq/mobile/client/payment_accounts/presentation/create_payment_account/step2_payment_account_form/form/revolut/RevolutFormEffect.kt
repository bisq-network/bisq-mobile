package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.revolut

import network.bisq.mobile.client.payment_accounts.domain.model.fiat.revolut.CreateRevolutAccount

sealed interface RevolutFormEffect {
    data class NavigateToNextScreen(
        val account: CreateRevolutAccount,
    ) : RevolutFormEffect
}
