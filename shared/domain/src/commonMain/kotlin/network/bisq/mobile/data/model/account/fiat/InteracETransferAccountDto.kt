package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class InteracETransferAccountDto(
    override val accountName: String,
    override val accountPayload: InteracETransferAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.INTERAC_E_TRANSFER,
) : FiatAccountDto
