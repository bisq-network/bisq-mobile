package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class UserDefinedFiatAccountDto(
    override val accountName: String,
    override val accountPayload: UserDefinedFiatAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.CUSTOM,
) : FiatAccountDto
