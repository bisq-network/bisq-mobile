package network.bisq.mobile.client.payment_accounts.data.model.fiat.revolut

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.payment_accounts.data.model.PaymentAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.FiatPaymentRailDto

@Serializable
data class RevolutAccountDto(
    override val accountName: String,
    override val accountPayload: RevolutAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.REVOLUT,
    override val creationDate: String? = null,
    override val tradeLimitInfo: String,
    override val tradeDuration: String,
) : PaymentAccountDto
