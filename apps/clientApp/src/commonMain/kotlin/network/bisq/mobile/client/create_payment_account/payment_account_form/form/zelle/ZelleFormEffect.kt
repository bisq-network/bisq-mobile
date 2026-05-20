package network.bisq.mobile.client.create_payment_account.payment_account_form.form.zelle

import network.bisq.mobile.domain.model.account.create.fiat.CreateZelleAccount

sealed interface ZelleFormEffect {
    data class NavigateToNextScreen(
        val account: CreateZelleAccount,
    ) : ZelleFormEffect
}
