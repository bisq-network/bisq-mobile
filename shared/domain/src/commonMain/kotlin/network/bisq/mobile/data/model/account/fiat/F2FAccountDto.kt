package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class F2FAccountDto(
    override val accountName: String,
    override val accountPayload: F2FAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.F2F,
) : FiatAccountDto
