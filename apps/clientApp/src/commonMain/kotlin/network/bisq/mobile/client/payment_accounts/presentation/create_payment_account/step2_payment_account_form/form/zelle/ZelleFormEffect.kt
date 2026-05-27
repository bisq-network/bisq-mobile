package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.zelle

import network.bisq.mobile.client.payment_accounts.domain.model.fiat.zelle.CreateZelleAccount

sealed interface ZelleFormEffect {
    data class NavigateToNextScreen(
        val account: CreateZelleAccount,
    ) : ZelleFormEffect
}
