package network.bisq.mobile.client.payment_accounts.data.model.fiat.zelle

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.payment_accounts.data.model.PaymentAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.FiatPaymentRailDto

@Serializable
data class ZelleAccountDto(
    override val accountName: String,
    override val accountPayload: ZelleAccountPayloadDto,
    override val paymentRail: FiatPaymentRailDto = FiatPaymentRailDto.ZELLE,
    override val creationDate: String? = null,
    override val tradeLimitInfo: String? = null,
    override val tradeDuration: String? = null,
) : PaymentAccountDto
