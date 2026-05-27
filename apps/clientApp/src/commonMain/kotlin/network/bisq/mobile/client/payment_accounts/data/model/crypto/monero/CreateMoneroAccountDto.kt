package network.bisq.mobile.client.payment_accounts.data.model.crypto.monero

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.payment_accounts.data.model.create.CreatePaymentAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.crypto.CryptoPaymentRailDto

@Serializable
data class CreateMoneroAccountDto(
    override val accountName: String,
    override val paymentRail: CryptoPaymentRailDto = CryptoPaymentRailDto.MONERO,
    override val accountPayload: CreateMoneroAccountPayloadDto,
) : CreatePaymentAccountDto
