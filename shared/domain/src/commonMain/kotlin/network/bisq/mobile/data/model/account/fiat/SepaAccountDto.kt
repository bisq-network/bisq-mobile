package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class SepaAccountDto(
    override val accountName: String,
    override val accountPayload: SepaAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.SEPA,
) : FiatAccountDto
