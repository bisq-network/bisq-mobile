package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class PerfectMoneyAccountDto(
    override val accountName: String,
    override val accountPayload: PerfectMoneyAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.PERFECT_MONEY,
) : FiatAccountDto
