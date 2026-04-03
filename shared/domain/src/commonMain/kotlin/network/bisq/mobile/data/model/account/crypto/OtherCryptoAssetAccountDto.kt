package network.bisq.mobile.data.model.account.crypto

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.model.account.PaymentAccountDto
import network.bisq.mobile.data.model.account.PaymentRailDto

@Serializable
data class OtherCryptoAssetAccountDto(
    override val accountName: String,
    override val accountPayload: OtherCryptoAssetAccountPayloadDto,
    override val paymentRail: PaymentRailDto = CryptoPaymentRailDto.OTHER_CRYPTO_ASSET,
    override val creationDate: String? = null,
    override val tradeLimitInfo: String? = null,
    override val tradeDuration: String? = null,
) : PaymentAccountDto
