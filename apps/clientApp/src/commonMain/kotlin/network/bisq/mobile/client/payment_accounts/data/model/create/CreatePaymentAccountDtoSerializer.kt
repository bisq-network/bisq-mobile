package network.bisq.mobile.client.payment_accounts.data.model.create

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import network.bisq.mobile.client.payment_accounts.data.model.crypto.CryptoPaymentRailDto
import network.bisq.mobile.client.payment_accounts.data.model.crypto.monero.CreateMoneroAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.crypto.other_crypto.CreateOtherCryptoAssetAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.ach_transfer.CreateAchTransferAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.cash_deposit.CreateCashDepositAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.FiatPaymentRailDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.revolut.CreateRevolutAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.user_defined.CreateUserDefinedFiatAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.wise.CreateWiseAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.zelle.CreateZelleAccountDto

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
            FiatPaymentRailDto.ACH_TRANSFER.name -> CreateAchTransferAccountDto.serializer()
            FiatPaymentRailDto.CASH_DEPOSIT.name -> CreateCashDepositAccountDto.serializer()
            FiatPaymentRailDto.ZELLE.name -> CreateZelleAccountDto.serializer()
            FiatPaymentRailDto.WISE.name -> CreateWiseAccountDto.serializer()
            FiatPaymentRailDto.REVOLUT.name -> CreateRevolutAccountDto.serializer()
            CryptoPaymentRailDto.MONERO.name -> CreateMoneroAccountDto.serializer()
            CryptoPaymentRailDto.OTHER_CRYPTO_ASSET.name -> CreateOtherCryptoAssetAccountDto.serializer()
            else -> throw IllegalArgumentException(
                "Unsupported create payment rail: $paymentRailValue.",
            )
        }
    }
}
