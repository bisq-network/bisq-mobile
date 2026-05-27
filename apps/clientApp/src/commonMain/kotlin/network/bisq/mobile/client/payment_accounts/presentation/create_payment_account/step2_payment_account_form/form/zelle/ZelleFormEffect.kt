package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.zelle

import network.bisq.mobile.domain.model.account.create.fiat.CreateZelleAccount

sealed interface ZelleFormEffect {
    data class NavigateToNextScreen(
        val account: CreateZelleAccount,
    ) : ZelleFormEffect
}
