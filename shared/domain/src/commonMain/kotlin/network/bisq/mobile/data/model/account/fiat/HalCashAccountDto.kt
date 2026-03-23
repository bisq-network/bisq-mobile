package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class HalCashAccountDto(
    override val accountName: String,
    override val accountPayload: HalCashAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.HAL_CASH,
) : FiatAccountDto
