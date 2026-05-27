package network.bisq.mobile.client.payment_accounts.data.model.crypto.monero

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.payment_accounts.data.model.PaymentAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.crypto.CryptoPaymentRailDto

@Serializable
data class MoneroAccountDto(
    override val accountName: String,
    override val accountPayload: MoneroAccountPayloadDto,
    override val paymentRail: CryptoPaymentRailDto = CryptoPaymentRailDto.MONERO,
    override val creationDate: String? = null,
    override val tradeLimitInfo: String? = null,
    override val tradeDuration: String? = null,
) : PaymentAccountDto
