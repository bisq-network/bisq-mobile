package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class NeftAccountDto(
    override val accountName: String,
    override val accountPayload: NeftAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.NEFT,
) : FiatAccountDto
