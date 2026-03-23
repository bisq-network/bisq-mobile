package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class WiseAccountDto(
    override val accountName: String,
    override val accountPayload: WiseAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.WISE,
) : FiatAccountDto
