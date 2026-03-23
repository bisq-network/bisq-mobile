package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class UpiAccountDto(
    override val accountName: String,
    override val accountPayload: UpiAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.UPI,
) : FiatAccountDto
