package network.bisq.mobile.data.model.account.crypto.create

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.model.account.create.CreatePaymentAccountDto
import network.bisq.mobile.data.model.account.crypto.CryptoPaymentRailDto

@Serializable
data class CreateOtherCryptoAssetAccountDto(
    override val accountName: String,
    override val paymentRail: CryptoPaymentRailDto = CryptoPaymentRailDto.OTHER_CRYPTO_ASSET,
    override val accountPayload: CreateOtherCryptoAssetAccountPayloadDto,
) : CreatePaymentAccountDto
