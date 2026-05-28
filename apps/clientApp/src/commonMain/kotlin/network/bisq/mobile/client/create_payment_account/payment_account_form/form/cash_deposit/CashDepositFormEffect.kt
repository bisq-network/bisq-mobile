package network.bisq.mobile.client.create_payment_account.payment_account_form.form.cash_deposit

import network.bisq.mobile.domain.model.account.create.fiat.CreateCashDepositAccount

sealed interface CashDepositFormEffect {
    data class NavigateToNextScreen(
        val account: CreateCashDepositAccount,
    ) : CashDepositFormEffect
}
