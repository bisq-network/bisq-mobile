package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.cash_deposit

import network.bisq.mobile.domain.model.account.create.fiat.CreateCashDepositAccount

sealed interface CashDepositFormEffect {
    data class NavigateToNextScreen(
        val account: CreateCashDepositAccount,
    ) : CashDepositFormEffect
}
