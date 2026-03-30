package network.bisq.mobile.data.model.account

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import network.bisq.mobile.data.model.account.crypto.CryptoPaymentRailDto
import network.bisq.mobile.data.model.account.crypto.MoneroAccountDto
import network.bisq.mobile.data.model.account.crypto.OtherCryptoAssetAccountDto
import network.bisq.mobile.data.model.account.fiat.FiatPaymentRailDto
import network.bisq.mobile.data.model.account.fiat.UserDefinedFiatAccountDto
import network.bisq.mobile.data.model.account.fiat.ZelleAccountDto

/**
 * Custom serializer for PaymentAccountDto that uses content-based polymorphic deserialization.
 * Determines the concrete type based on the 'paymentRail' field in the JSON.
 */
object PaymentAccountDtoSerializer : JsonContentPolymorphicSerializer<PaymentAccountDto>(PaymentAccountDto::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<PaymentAccountDto> {
        val paymentRailValue =
            element.jsonObject["paymentRail"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("Missing 'paymentRail' field in PaymentAccountDto JSON")

        return when (paymentRailValue) {
            // Fiat
            FiatPaymentRailDto.CUSTOM.name -> UserDefinedFiatAccountDto.serializer()
            FiatPaymentRailDto.ZELLE.name -> ZelleAccountDto.serializer()
            // Crypto
            CryptoPaymentRailDto.MONERO.name -> MoneroAccountDto.serializer()
            CryptoPaymentRailDto.OTHER_CRYPTO_ASSET.name -> OtherCryptoAssetAccountDto.serializer()

            else -> throw IllegalArgumentException(
                "Unsupported or not yet implemented payment rail: $paymentRailValue.",
            )
        }
    }
}
