package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Custom serializer for FiatAccountDto that uses content-based polymorphic deserialization.
 * Determines the concrete type based on the 'paymentRail' field in the JSON.
 */
object FiatAccountDtoSerializer : JsonContentPolymorphicSerializer<FiatAccountDto>(FiatAccountDto::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<FiatAccountDto> {
        val paymentRailValue =
            element.jsonObject["paymentRail"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("Missing 'paymentRail' field in FiatAccountDto JSON")

        return when (paymentRailValue) {
            FiatPaymentRailDto.CUSTOM.name -> UserDefinedFiatAccountDto.serializer()
            FiatPaymentRailDto.ZELLE.name -> ZelleAccountDto.serializer()
            else -> throw IllegalArgumentException(
                "Unsupported or not yet implemented payment rail: $paymentRailValue.",
            )
        }
    }
}
