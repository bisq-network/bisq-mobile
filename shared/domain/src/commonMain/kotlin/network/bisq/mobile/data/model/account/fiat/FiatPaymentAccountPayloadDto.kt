package network.bisq.mobile.data.model.account.fiat

import network.bisq.mobile.data.model.account.PaymentAccountPayloadDto

interface FiatPaymentAccountPayloadDto : PaymentAccountPayloadDto {
    val chargebackRisk: FiatPaymentMethodChargebackRiskDto?
    val paymentMethodName: String?
    val currency: String?
    val country: String?
}
