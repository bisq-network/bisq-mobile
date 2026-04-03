package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.model.account.PaymentAccountDto

@Serializable
data class UserDefinedFiatAccountDto(
    override val accountName: String,
    override val accountPayload: UserDefinedFiatAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.CUSTOM,
    override val tradeLimitInfo: String? = null,
    override val tradeDuration: String? = null,
    override val creationDate: String? = null,
) : PaymentAccountDto
