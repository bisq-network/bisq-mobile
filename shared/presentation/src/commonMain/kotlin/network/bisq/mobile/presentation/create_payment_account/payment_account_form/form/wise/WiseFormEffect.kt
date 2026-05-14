package network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.wise

import network.bisq.mobile.domain.model.account.create.fiat.CreateWiseAccount

sealed interface WiseFormEffect {
    data class NavigateToNextScreen(
        val account: CreateWiseAccount,
    ) : WiseFormEffect
}
