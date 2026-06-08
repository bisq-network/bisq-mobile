package network.bisq.mobile.client.payment_accounts.domain.model.fiat.same_bank

import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccountPayload
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccountType
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.Country
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatCurrency
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk

data class SameBankAccountPayload(
    override val chargebackRisk: FiatPaymentMethodChargebackRisk? = null,
    override val paymentMethodName: String,
    override val currency: FiatCurrency,
    override val country: Country,
    override val holderName: String? = null,
    override val holderId: String? = null,
    override val bankName: String? = null,
    override val bankId: String? = null,
    override val branchId: String? = null,
    override val accountNr: String,
    override val bankAccountType: BankAccountType? = null,
    override val nationalAccountId: String? = null,
) : BankAccountPayload
