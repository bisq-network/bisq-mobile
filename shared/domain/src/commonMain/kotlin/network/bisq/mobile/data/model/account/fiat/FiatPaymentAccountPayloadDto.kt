package network.bisq.mobile.data.model.account.fiat

import network.bisq.mobile.data.model.account.PaymentAccountPayloadDto

interface FiatPaymentAccountPayloadDto : PaymentAccountPayloadDto {
    val paymentMethodName: String
    val chargebackRisk: FiatPaymentMethodChargebackRiskDto?
}
