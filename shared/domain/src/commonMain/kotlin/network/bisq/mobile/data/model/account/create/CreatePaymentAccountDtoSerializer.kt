package network.bisq.mobile.data.model.account.create

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import network.bisq.mobile.data.model.account.crypto.CryptoPaymentRailDto
import network.bisq.mobile.data.model.account.crypto.create.CreateMoneroAccountDto
import network.bisq.mobile.data.model.account.crypto.create.CreateOtherCryptoAssetAccountDto
import network.bisq.mobile.data.model.account.fiat.FiatPaymentRailDto
import network.bisq.mobile.data.model.account.fiat.create.CreateUserDefinedFiatAccountDto
import network.bisq.mobile.data.model.account.fiat.create.CreateZelleAccountDto

/**
 * Custom serializer for create payment account DTOs that uses the paymentRail field
 * to select the concrete rail-specific create request DTO.
 */
object CreatePaymentAccountDtoSerializer : JsonContentPolymorphicSerializer<CreatePaymentAccountDto>(CreatePaymentAccountDto::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<CreatePaymentAccountDto> {
        val paymentRailValue =
            element.jsonObject["paymentRail"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("Missing 'paymentRail' field in CreatePaymentAccountDto JSON")

        return when (paymentRailValue) {
            FiatPaymentRailDto.CUSTOM.name -> CreateUserDefinedFiatAccountDto.serializer()
            FiatPaymentRailDto.ZELLE.name -> CreateZelleAccountDto.serializer()
            CryptoPaymentRailDto.MONERO.name -> CreateMoneroAccountDto.serializer()
            CryptoPaymentRailDto.OTHER_CRYPTO_ASSET.name -> CreateOtherCryptoAssetAccountDto.serializer()
            else -> throw IllegalArgumentException(
                "Unsupported create payment rail: $paymentRailValue.",
            )
        }
    }
}
