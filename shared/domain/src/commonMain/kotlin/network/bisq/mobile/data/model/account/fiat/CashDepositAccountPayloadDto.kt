package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

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
