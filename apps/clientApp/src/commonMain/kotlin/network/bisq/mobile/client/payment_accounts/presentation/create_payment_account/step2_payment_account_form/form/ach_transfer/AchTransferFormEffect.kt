package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.ach_transfer

import network.bisq.mobile.client.payment_accounts.domain.model.fiat.ach_transfer.CreateAchTransferAccount

sealed interface AchTransferFormEffect {
    data class NavigateToNextScreen(
        val account: CreateAchTransferAccount,
    ) : AchTransferFormEffect
}
