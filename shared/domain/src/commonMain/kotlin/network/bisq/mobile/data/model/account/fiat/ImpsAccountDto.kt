package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class ImpsAccountDto(
    override val accountName: String,
    override val accountPayload: ImpsAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.IMPS,
) : FiatAccountDto
