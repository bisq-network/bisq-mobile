package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.model.account.PaymentAccountDto

@Serializable
data class RevolutAccountDto(
    override val accountName: String,
    override val accountPayload: RevolutAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.REVOLUT,
    override val creationDate: String? = null,
    override val tradeLimitInfo: String,
    override val tradeDuration: String,
) : PaymentAccountDto
