package network.bisq.mobile.client.payment_accounts.data.model.fiat.cash_deposit

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.BankAccountTypeDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.CountryDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.FiatCurrencyDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.FiatPaymentAccountPayloadDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.payment_method.FiatPaymentMethodChargebackRiskDto

@Serializable
data class CashDepositAccountPayloadDto(
    override val chargebackRisk: FiatPaymentMethodChargebackRiskDto? = null,
    override val paymentMethodName: String,
    val currency: FiatCurrencyDto,
    val country: CountryDto,
    val holderName: String,
    val holderId: String? = null,
    val bankName: String,
    val bankId: String? = null,
    val branchId: String? = null,
    val accountNr: String,
    val bankAccountType: BankAccountTypeDto? = null,
    val nationalAccountId: String? = null,
    val requirements: String? = null,
) : FiatPaymentAccountPayloadDto
