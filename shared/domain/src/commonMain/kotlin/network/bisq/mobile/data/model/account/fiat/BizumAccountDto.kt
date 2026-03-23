package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class BizumAccountDto(
    override val accountName: String,
    override val accountPayload: BizumAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.BIZUM,
) : FiatAccountDto
