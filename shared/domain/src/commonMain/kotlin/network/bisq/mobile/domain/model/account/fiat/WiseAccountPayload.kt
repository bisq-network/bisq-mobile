package network.bisq.mobile.domain.model.account.fiat

data class WiseAccountPayload(
    val selectedCurrencies: List<FiatCurrency>,
    val holderName: String,
    val email: String,
    override val chargebackRisk: FiatPaymentMethodChargebackRisk? = null,
    override val paymentMethodName: String,
) : FiatPaymentAccountPayload
