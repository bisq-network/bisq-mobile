package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class SepaInstantAccountDto(
    override val accountName: String,
    override val accountPayload: SepaInstantAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.SEPA_INSTANT,
) : FiatAccountDto
