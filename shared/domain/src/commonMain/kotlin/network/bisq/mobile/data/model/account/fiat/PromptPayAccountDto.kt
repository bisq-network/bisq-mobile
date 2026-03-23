package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class PromptPayAccountDto(
    override val accountName: String,
    override val accountPayload: PromptPayAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.PROMPT_PAY,
) : FiatAccountDto
