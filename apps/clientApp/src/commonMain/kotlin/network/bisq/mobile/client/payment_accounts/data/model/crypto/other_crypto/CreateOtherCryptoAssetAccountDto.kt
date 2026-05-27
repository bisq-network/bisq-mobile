package network.bisq.mobile.client.payment_accounts.data.model.crypto.other_crypto

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.payment_accounts.data.model.create.CreatePaymentAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.crypto.CryptoPaymentRailDto

@Serializable
data class CreateOtherCryptoAssetAccountDto(
    override val accountName: String,
    override val paymentRail: CryptoPaymentRailDto = CryptoPaymentRailDto.OTHER_CRYPTO_ASSET,
    override val accountPayload: CreateOtherCryptoAssetAccountPayloadDto,
) : CreatePaymentAccountDto
