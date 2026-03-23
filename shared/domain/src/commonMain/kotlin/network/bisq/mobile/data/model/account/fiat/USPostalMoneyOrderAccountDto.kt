package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class USPostalMoneyOrderAccountDto(
    override val accountName: String,
    override val accountPayload: USPostalMoneyOrderAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.US_POSTAL_MONEY_ORDER,
) : FiatAccountDto
