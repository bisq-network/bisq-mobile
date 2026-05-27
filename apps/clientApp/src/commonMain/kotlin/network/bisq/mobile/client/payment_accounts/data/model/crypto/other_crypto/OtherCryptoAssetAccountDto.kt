package network.bisq.mobile.client.payment_accounts.data.model.crypto.other_crypto

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.payment_accounts.data.model.PaymentAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.crypto.CryptoPaymentRailDto

@Serializable
data class OtherCryptoAssetAccountDto(
    override val accountName: String,
    override val accountPayload: OtherCryptoAssetAccountPayloadDto,
    override val paymentRail: CryptoPaymentRailDto = CryptoPaymentRailDto.OTHER_CRYPTO_ASSET,
    override val creationDate: String? = null,
    override val tradeLimitInfo: String? = null,
    override val tradeDuration: String? = null,
) : PaymentAccountDto
