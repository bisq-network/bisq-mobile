package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class MoneyBeamAccountDto(
    override val accountName: String,
    override val accountPayload: MoneyBeamAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.MONEY_BEAM,
) : FiatAccountDto
