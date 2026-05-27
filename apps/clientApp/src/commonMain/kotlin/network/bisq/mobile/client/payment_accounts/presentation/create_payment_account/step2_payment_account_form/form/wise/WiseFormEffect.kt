package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.wise

import network.bisq.mobile.domain.model.account.create.fiat.CreateWiseAccount

sealed interface WiseFormEffect {
    data class NavigateToNextScreen(
        val account: CreateWiseAccount,
    ) : WiseFormEffect
}
