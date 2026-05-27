package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.wise

import network.bisq.mobile.client.payment_accounts.domain.model.fiat.wise.CreateWiseAccount

sealed interface WiseFormEffect {
    data class NavigateToNextScreen(
        val account: CreateWiseAccount,
    ) : WiseFormEffect
}
