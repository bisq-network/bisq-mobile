package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class RevolutAccountDto(
    override val accountName: String,
    override val accountPayload: RevolutAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.REVOLUT,
) : FiatAccountDto
