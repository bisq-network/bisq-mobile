package network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank

import network.bisq.mobile.domain.model.account.fiat.FiatPaymentAccount

interface BankAccount : FiatPaymentAccount {
    override val accountPayload: BankAccountPayload
}
