package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class PayIdAccountDto(
    override val accountName: String,
    override val accountPayload: PayIdAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.PAY_ID,
) : FiatAccountDto
