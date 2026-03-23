package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class PixAccountDto(
    override val accountName: String,
    override val accountPayload: PixAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.PIX,
) : FiatAccountDto
