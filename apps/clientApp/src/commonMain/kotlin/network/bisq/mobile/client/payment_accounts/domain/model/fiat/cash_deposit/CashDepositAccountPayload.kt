package network.bisq.mobile.client.payment_accounts.domain.model.fiat.cash_deposit

import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccountType
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.Country
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.FiatPaymentCountryBasedAccountPayload
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatCurrency
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatPaymentSingleCurrencyAccountPayload
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentAccountPayload
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk

data class CashDepositAccountPayload(
    override val chargebackRisk: FiatPaymentMethodChargebackRisk? = null,
    override val paymentMethodName: String,
    override val currency: FiatCurrency,
    override val country: Country,
    val holderName: String,
    val holderId: String? = null,
    val bankName: String,
    val bankId: String? = null,
    val branchId: String? = null,
    val accountNr: String,
    val bankAccountType: BankAccountType? = null,
    val nationalAccountId: String? = null,
    val requirements: String? = null,
) : FiatPaymentAccountPayload,
    FiatPaymentCountryBasedAccountPayload,
    FiatPaymentSingleCurrencyAccountPayload
