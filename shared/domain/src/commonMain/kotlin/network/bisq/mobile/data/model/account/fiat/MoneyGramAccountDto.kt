package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class MoneyGramAccountDto(
    override val accountName: String,
    override val accountPayload: MoneyGramAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.MONEY_GRAM,
) : FiatAccountDto
