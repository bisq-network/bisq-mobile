package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class SwishAccountDto(
    override val accountName: String,
    override val accountPayload: SwishAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.SWISH,
) : FiatAccountDto
