package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class AchTransferAccountDto(
    override val accountName: String,
    override val accountPayload: AchTransferAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.ACH_TRANSFER,
) : FiatAccountDto
