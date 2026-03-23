package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class MercadoPagoAccountDto(
    override val accountName: String,
    override val accountPayload: MercadoPagoAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.MERCADO_PAGO,
) : FiatAccountDto
