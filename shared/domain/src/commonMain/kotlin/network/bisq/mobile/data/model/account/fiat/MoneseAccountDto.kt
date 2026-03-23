package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class MoneseAccountDto(
    override val accountName: String,
    override val accountPayload: MoneseAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.MONESE,
) : FiatAccountDto
