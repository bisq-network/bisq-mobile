package network.bisq.mobile.client.payment_accounts.data.model.fiat.common

import network.bisq.mobile.client.payment_accounts.data.model.PaymentAccountPayloadDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.payment_method.FiatPaymentMethodChargebackRiskDto

interface FiatPaymentAccountPayloadDto : PaymentAccountPayloadDto {
    val paymentMethodName: String
    val chargebackRisk: FiatPaymentMethodChargebackRiskDto?
}
