package network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank

import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.Country
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.FiatPaymentCountryBasedAccountPayload
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatCurrency
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatPaymentSingleCurrencyAccountPayload
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentAccountPayload

interface BankAccountPayload :
    FiatPaymentAccountPayload,
    FiatPaymentCountryBasedAccountPayload,
    FiatPaymentSingleCurrencyAccountPayload {
    override val currency: FiatCurrency
    override val country: Country
    val holderName: String?
    val holderId: String?
    val bankName: String?
    val bankId: String?
    val branchId: String?
    val accountNr: String
    val bankAccountType: BankAccountType?
    val nationalAccountId: String?
}
