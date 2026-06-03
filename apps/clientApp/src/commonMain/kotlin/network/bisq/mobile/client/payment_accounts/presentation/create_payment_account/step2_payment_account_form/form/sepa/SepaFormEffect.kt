package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.sepa

import network.bisq.mobile.client.payment_accounts.domain.model.fiat.sepa.CreateSepaAccount

sealed interface SepaFormEffect {
    data class NavigateToNextScreen(
        val account: CreateSepaAccount,
    ) : SepaFormEffect
}
