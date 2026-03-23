package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class UpholdAccountDto(
    override val accountName: String,
    override val accountPayload: UpholdAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.UPHOLD,
) : FiatAccountDto
