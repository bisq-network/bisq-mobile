package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class AliPayAccountDto(
    override val accountName: String,
    override val accountPayload: AliPayAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.ALI_PAY,
) : FiatAccountDto
