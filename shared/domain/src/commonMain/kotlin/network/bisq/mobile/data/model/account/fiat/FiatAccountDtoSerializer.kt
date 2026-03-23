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
            FiatPaymentRailDto.UPHOLD.name -> UpholdAccountDto.serializer()
            FiatPaymentRailDto.ADVANCED_CASH.name -> AdvancedCashAccountDto.serializer()
            FiatPaymentRailDto.ALI_PAY.name -> AliPayAccountDto.serializer()
            FiatPaymentRailDto.AMAZON_GIFT_CARD.name -> AmazonGiftCardAccountDto.serializer()
            FiatPaymentRailDto.ACH_TRANSFER.name -> AchTransferAccountDto.serializer()
            FiatPaymentRailDto.NATIONAL_BANK.name -> NationalBankAccountDto.serializer()
            FiatPaymentRailDto.CASH_DEPOSIT.name -> CashDepositAccountDto.serializer()
            FiatPaymentRailDto.SAME_BANK.name -> SameBankAccountDto.serializer()
            FiatPaymentRailDto.DOMESTIC_WIRE_TRANSFER.name -> DomesticWireTransferAccountDto.serializer()
            FiatPaymentRailDto.BIZUM.name -> BizumAccountDto.serializer()
            FiatPaymentRailDto.CASH_BY_MAIL.name -> CashByMailAccountDto.serializer()
            FiatPaymentRailDto.F2F.name -> F2FAccountDto.serializer()
            FiatPaymentRailDto.FASTER_PAYMENTS.name -> FasterPaymentsAccountDto.serializer()
            FiatPaymentRailDto.HAL_CASH.name -> HalCashAccountDto.serializer()
            FiatPaymentRailDto.IMPS.name -> ImpsAccountDto.serializer()
            FiatPaymentRailDto.INTERAC_E_TRANSFER.name -> InteracETransferAccountDto.serializer()
            FiatPaymentRailDto.MERCADO_PAGO.name -> MercadoPagoAccountDto.serializer()
            FiatPaymentRailDto.MONEY_BEAM.name -> MoneyBeamAccountDto.serializer()
            FiatPaymentRailDto.MONEY_GRAM.name -> MoneyGramAccountDto.serializer()
            FiatPaymentRailDto.MONESE.name -> MoneseAccountDto.serializer()
            FiatPaymentRailDto.NEFT.name -> NeftAccountDto.serializer()
            FiatPaymentRailDto.PAY_ID.name -> PayIdAccountDto.serializer()
            FiatPaymentRailDto.PAYSERA.name -> PayseraAccountDto.serializer()
            FiatPaymentRailDto.PERFECT_MONEY.name -> PerfectMoneyAccountDto.serializer()
            FiatPaymentRailDto.PIN_4.name -> Pin4AccountDto.serializer()
            FiatPaymentRailDto.PIX.name -> PixAccountDto.serializer()
            FiatPaymentRailDto.PROMPT_PAY.name -> PromptPayAccountDto.serializer()
            FiatPaymentRailDto.REVOLUT.name -> RevolutAccountDto.serializer()
            FiatPaymentRailDto.SATISPAY.name -> SatispayAccountDto.serializer()
            FiatPaymentRailDto.SBP.name -> SbpAccountDto.serializer()
            FiatPaymentRailDto.SEPA.name -> SepaAccountDto.serializer()
            FiatPaymentRailDto.SEPA_INSTANT.name -> SepaInstantAccountDto.serializer()
            FiatPaymentRailDto.STRIKE.name -> StrikeAccountDto.serializer()
            FiatPaymentRailDto.SWIFT.name -> SwiftAccountDto.serializer()
            FiatPaymentRailDto.SWISH.name -> SwishAccountDto.serializer()
            FiatPaymentRailDto.UPI.name -> UpiAccountDto.serializer()
            FiatPaymentRailDto.US_POSTAL_MONEY_ORDER.name -> USPostalMoneyOrderAccountDto.serializer()
            FiatPaymentRailDto.WECHAT_PAY.name -> WeChatPayAccountDto.serializer()
            FiatPaymentRailDto.WISE.name -> WiseAccountDto.serializer()
            FiatPaymentRailDto.WISE_USD.name -> WiseUsdAccountDto.serializer()
            else -> throw IllegalArgumentException(
                "Unsupported or not yet implemented payment rail: $paymentRailValue.",
            )
        }
    }
}
