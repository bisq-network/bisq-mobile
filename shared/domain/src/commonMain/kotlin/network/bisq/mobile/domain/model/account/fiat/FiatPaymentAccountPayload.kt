package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.domain.model.account.PaymentAccountPayload

interface FiatPaymentAccountPayload : PaymentAccountPayload {
    val chargebackRisk: FiatPaymentMethodChargebackRisk?
    val paymentMethodName: String
    val currency: String
    val country: String?
}
