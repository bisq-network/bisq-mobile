package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class StrikeAccountDto(
    override val accountName: String,
    override val accountPayload: StrikeAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.STRIKE,
) : FiatAccountDto
