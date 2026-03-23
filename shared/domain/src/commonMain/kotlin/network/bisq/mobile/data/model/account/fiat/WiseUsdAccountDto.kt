package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class WiseUsdAccountDto(
    override val accountName: String,
    override val accountPayload: WiseUsdAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.WISE_USD,
) : FiatAccountDto
