package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class ZelleAccountDto(
    override val accountName: String,
    override val accountPayload: ZelleAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.ZELLE,
) : FiatAccountDto
