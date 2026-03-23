package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class AdvancedCashAccountDto(
    override val accountName: String,
    override val accountPayload: AdvancedCashAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.ADVANCED_CASH,
) : FiatAccountDto
