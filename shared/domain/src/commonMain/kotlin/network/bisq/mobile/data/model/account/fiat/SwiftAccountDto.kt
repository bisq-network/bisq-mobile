package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class SwiftAccountDto(
    override val accountName: String,
    override val accountPayload: SwiftAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.SWIFT,
) : FiatAccountDto
