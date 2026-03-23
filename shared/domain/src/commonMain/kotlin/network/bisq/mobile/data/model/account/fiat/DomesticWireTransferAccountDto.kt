package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class DomesticWireTransferAccountDto(
    override val accountName: String,
    override val accountPayload: DomesticWireTransferAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.DOMESTIC_WIRE_TRANSFER,
) : FiatAccountDto
