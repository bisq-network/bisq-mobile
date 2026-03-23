package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class WeChatPayAccountDto(
    override val accountName: String,
    override val accountPayload: WeChatPayAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.WECHAT_PAY,
) : FiatAccountDto
