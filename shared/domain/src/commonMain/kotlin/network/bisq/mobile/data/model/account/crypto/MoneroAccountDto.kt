package network.bisq.mobile.data.model.account.crypto

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.model.account.PaymentAccountDto

@Serializable
data class MoneroAccountDto(
    override val accountName: String,
    override val accountPayload: MoneroAccountPayloadDto,
    override val paymentRail: CryptoPaymentRailDto = CryptoPaymentRailDto.MONERO,
    override val creationDate: Long? = null,
) : PaymentAccountDto
