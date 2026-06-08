package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.bank

import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount

sealed interface BankAccountFormEffect<out T : CreatePaymentAccount> {
    data class NavigateToNextScreen<T : CreatePaymentAccount>(
        val account: T,
    ) : BankAccountFormEffect<T>
}
