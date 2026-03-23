package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class SbpAccountDto(
    override val accountName: String,
    override val accountPayload: SbpAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.SBP,
) : FiatAccountDto
