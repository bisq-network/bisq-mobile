package network.bisq.mobile.domain.model.account.fiat

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
