package network.bisq.mobile.domain.model.account.fiat

data class RevolutAccountPayload(
    val selectedCurrencies: List<FiatCurrency>,
    val userName: String,
    override val chargebackRisk: FiatPaymentMethodChargebackRisk? = null,
    override val paymentMethodName: String,
) : FiatPaymentAccountPayload
