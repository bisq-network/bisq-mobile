package network.bisq.mobile.i18n

// From payment_method.properties
data class PaymentMethodStrings(
    val NATIONAL_BANK: String,
    val INTERNATIONAL_BANK: String,
    val SAME_BANK: String,
    val SPECIFIC_BANKS: String,
    val US_POSTAL_MONEY_ORDER: String,
    val CASH_DEPOSIT: String,
    val CASH_BY_MAIL: String,
    val MONEY_GRAM: String,
    val WESTERN_UNION: String,
    val F2F: String,
    val JAPAN_BANK: String,
    val PAY_ID: String,
    val NATIONAL_BANK_SHORT: String,
    val INTERNATIONAL_BANK_SHORT: String,
    val SAME_BANK_SHORT: String,
    val SPECIFIC_BANKS_SHORT: String,
    val US_POSTAL_MONEY_ORDER_SHORT: String,
    val CASH_DEPOSIT_SHORT: String,
    val CASH_BY_MAIL_SHORT: String,
    val MONEY_GRAM_SHORT: String,
    val WESTERN_UNION_SHORT: String,
    val F2F_SHORT: String,
    val JAPAN_BANK_SHORT: String,
    val UPHOLD: String,
    val MONEY_BEAM: String,
    val POPMONEY: String,
    val REVOLUT: String,
    val CASH_APP: String,
    val PERFECT_MONEY: String,
    val ALI_PAY: String,
    val WECHAT_PAY: String,
    val SEPA: String,
    val SEPA_INSTANT: String,
    val FASTER_PAYMENTS: String,
    val SWISH: String,
    val ZELLE: String,
    val CHASE_QUICK_PAY: String,
    val INTERAC_E_TRANSFER: String,
    val HAL_CASH: String,
    val PROMPT_PAY: String,
    val ADVANCED_CASH: String,
    val WISE: String,
    val WISE_USD: String,
    val PAYSERA: String,
    val PAXUM: String,
    val NEFT: String,
    val RTGS: String,
    val IMPS: String,
    val UPI: String,
    val PAYTM: String,
    val NEQUI: String,
    val BIZUM: String,
    val PIX: String,
    val AMAZON_GIFT_CARD: String,
    val CAPITUAL: String,
    val CELPAY: String,
    val MONESE: String,
    val SATISPAY: String,
    val TIKKIE: String,
    val VERSE: String,
    val STRIKE: String,
    val SWIFT: String,
    val SWIFT_SHORT: String,
    val ACH_TRANSFER: String,
    val ACH_TRANSFER_SHORT: String,
    val DOMESTIC_WIRE_TRANSFER: String,
    val DOMESTIC_WIRE_TRANSFER_SHORT: String,
    val CIPS: String,
    val CIPS_SHORT: String,
    val NATIVE_CHAIN: String,
    val NATIVE_CHAIN_SHORT: String,
    val MAIN_CHAIN: String,
    val MAIN_CHAIN_SHORT: String,
    val LN: String,
    val LN_SHORT: String,
    val LBTC: String,
    val LBTC_SHORT: String,
    val RBTC: String,
    val RBTC_SHORT: String,
    val WBTC: String,
    val WBTC_SHORT: String,
    val OTHER: String,
)

// TODO Would be better to find a way to access the string dynamically without custom mapping as that is hard to maintain.
// AI generated mapping
fun PaymentMethodStrings.toDisplayString(paymentMethod: String, useShort: Boolean = true): String {
    val shortVersion = "${paymentMethod}_SHORT"
    val value: String = if (useShort) shortVersion else paymentMethod
    return toDisplayString(value)
}

private fun PaymentMethodStrings.toDisplayString(paymentMethod: String): String {
    return when (paymentMethod) {
        "NATIONAL_BANK" -> NATIONAL_BANK
        "INTERNATIONAL_BANK" -> INTERNATIONAL_BANK
        "SAME_BANK" -> SAME_BANK
        "SPECIFIC_BANKS" -> SPECIFIC_BANKS
        "US_POSTAL_MONEY_ORDER" -> US_POSTAL_MONEY_ORDER
        "CASH_DEPOSIT" -> CASH_DEPOSIT
        "CASH_BY_MAIL" -> CASH_BY_MAIL
        "MONEY_GRAM" -> MONEY_GRAM
        "WESTERN_UNION" -> WESTERN_UNION
        "F2F" -> F2F
        "JAPAN_BANK" -> JAPAN_BANK
        "PAY_ID" -> PAY_ID
        "NATIONAL_BANK_SHORT" -> NATIONAL_BANK_SHORT
        "INTERNATIONAL_BANK_SHORT" -> INTERNATIONAL_BANK_SHORT
        "SAME_BANK_SHORT" -> SAME_BANK_SHORT
        "SPECIFIC_BANKS_SHORT" -> SPECIFIC_BANKS_SHORT
        "US_POSTAL_MONEY_ORDER_SHORT" -> US_POSTAL_MONEY_ORDER_SHORT
        "CASH_DEPOSIT_SHORT" -> CASH_DEPOSIT_SHORT
        "CASH_BY_MAIL_SHORT" -> CASH_BY_MAIL_SHORT
        "MONEY_GRAM_SHORT" -> MONEY_GRAM_SHORT
        "WESTERN_UNION_SHORT" -> WESTERN_UNION_SHORT
        "F2F_SHORT" -> F2F_SHORT
        "JAPAN_BANK_SHORT" -> JAPAN_BANK_SHORT
        "UPHOLD" -> UPHOLD
        "MONEY_BEAM" -> MONEY_BEAM
        "POPMONEY" -> POPMONEY
        "REVOLUT" -> REVOLUT
        "CASH_APP" -> CASH_APP
        "PERFECT_MONEY" -> PERFECT_MONEY
        "ALI_PAY" -> ALI_PAY
        "WECHAT_PAY" -> WECHAT_PAY
        "SEPA" -> SEPA
        "SEPA_INSTANT" -> SEPA_INSTANT
        "FASTER_PAYMENTS" -> FASTER_PAYMENTS
        "SWISH" -> SWISH
        "ZELLE" -> ZELLE
        "CHASE_QUICK_PAY" -> CHASE_QUICK_PAY
        "INTERAC_E_TRANSFER" -> INTERAC_E_TRANSFER
        "HAL_CASH" -> HAL_CASH
        "PROMPT_PAY" -> PROMPT_PAY
        "ADVANCED_CASH" -> ADVANCED_CASH
        "WISE" -> WISE
        "WISE_USD" -> WISE_USD
        "PAYSERA" -> PAYSERA
        "PAXUM" -> PAXUM
        "NEFT" -> NEFT
        "RTGS" -> RTGS
        "IMPS" -> IMPS
        "UPI" -> UPI
        "PAYTM" -> PAYTM
        "NEQUI" -> NEQUI
        "BIZUM" -> BIZUM
        "PIX" -> PIX
        "AMAZON_GIFT_CARD" -> AMAZON_GIFT_CARD
        "CAPITUAL" -> CAPITUAL
        "CELPAY" -> CELPAY
        "MONESE" -> MONESE
        "SATISPAY" -> SATISPAY
        "TIKKIE" -> TIKKIE
        "VERSE" -> VERSE
        "STRIKE" -> STRIKE
        "SWIFT" -> SWIFT
        "SWIFT_SHORT" -> SWIFT_SHORT
        "ACH_TRANSFER" -> ACH_TRANSFER
        "ACH_TRANSFER_SHORT" -> ACH_TRANSFER_SHORT
        "DOMESTIC_WIRE_TRANSFER" -> DOMESTIC_WIRE_TRANSFER
        "DOMESTIC_WIRE_TRANSFER_SHORT" -> DOMESTIC_WIRE_TRANSFER_SHORT
        "CIPS" -> CIPS
        "CIPS_SHORT" -> CIPS_SHORT
        "NATIVE_CHAIN" -> NATIVE_CHAIN
        "NATIVE_CHAIN_SHORT" -> NATIVE_CHAIN_SHORT
        "MAIN_CHAIN" -> MAIN_CHAIN
        "MAIN_CHAIN_SHORT" -> MAIN_CHAIN_SHORT
        "LN" -> LN
        "LN_SHORT" -> LN_SHORT
        "LBTC" -> LBTC
        "LBTC_SHORT" -> LBTC_SHORT
        "RBTC" -> RBTC
        "RBTC_SHORT" -> RBTC_SHORT
        "WBTC" -> WBTC
        "WBTC_SHORT" -> WBTC_SHORT
        "OTHER" -> OTHER
        else -> {
            // We don't have short versions for all entries. If not found we get returned the input string. In that case we try again without the
            // _SHORT postfix
            if (paymentMethod.endsWith("_SHORT")) {
                toDisplayString(paymentMethod.replace("_SHORT", ""))
            } else {
                paymentMethod
            }
        }
    }
}
