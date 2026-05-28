package network.bisq.mobile.client.payment_accounts.data.model

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import network.bisq.mobile.client.payment_accounts.data.model.crypto.CryptoPaymentRailDto
import network.bisq.mobile.client.payment_accounts.data.model.crypto.monero.MoneroAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.crypto.other_crypto.OtherCryptoAssetAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.ach_transfer.AchTransferAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.cash_deposit.CashDepositAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.FiatPaymentRailDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.revolut.RevolutAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.user_defined.UserDefinedFiatAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.wise.WiseAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.zelle.ZelleAccountDto

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
            FiatPaymentRailDto.ACH_TRANSFER.name -> AchTransferAccountDto.serializer()
            FiatPaymentRailDto.CASH_DEPOSIT.name -> CashDepositAccountDto.serializer()
            FiatPaymentRailDto.ZELLE.name -> ZelleAccountDto.serializer()
            FiatPaymentRailDto.WISE.name -> WiseAccountDto.serializer()
            FiatPaymentRailDto.REVOLUT.name -> RevolutAccountDto.serializer()
            // Crypto
            CryptoPaymentRailDto.MONERO.name -> MoneroAccountDto.serializer()
            CryptoPaymentRailDto.OTHER_CRYPTO_ASSET.name -> OtherCryptoAssetAccountDto.serializer()

            else -> throw IllegalArgumentException(
                "Unsupported or not yet implemented payment rail: $paymentRailValue.",
            )
        }
    }
}
