package network.bisq.mobile.client.payment_accounts.data.model.fiat.wise

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.payment_accounts.data.model.PaymentAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.FiatPaymentRailDto

@Serializable
data class WiseAccountDto(
    override val accountName: String,
    override val accountPayload: WiseAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.WISE,
    override val creationDate: String? = null,
    override val tradeLimitInfo: String,
    override val tradeDuration: String,
) : PaymentAccountDto
