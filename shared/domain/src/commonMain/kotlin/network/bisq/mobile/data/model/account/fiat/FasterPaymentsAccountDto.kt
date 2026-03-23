package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class FasterPaymentsAccountDto(
    override val accountName: String,
    override val accountPayload: FasterPaymentsAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.FASTER_PAYMENTS,
) : FiatAccountDto
