package network.bisq.mobile.presentation.common.model.account

import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.payment_icon_ach
import bisqapps.shared.presentation.generated.resources.payment_icon_advanced_cash
import bisqapps.shared.presentation.generated.resources.payment_icon_alipay
import bisqapps.shared.presentation.generated.resources.payment_icon_amazon
import bisqapps.shared.presentation.generated.resources.payment_icon_bizum
import bisqapps.shared.presentation.generated.resources.payment_icon_cash_by_mail
import bisqapps.shared.presentation.generated.resources.payment_icon_cash_deposit
import bisqapps.shared.presentation.generated.resources.payment_icon_cashapp
import bisqapps.shared.presentation.generated.resources.payment_icon_domestic_wire_transfer
import bisqapps.shared.presentation.generated.resources.payment_icon_f2f
import bisqapps.shared.presentation.generated.resources.payment_icon_faster_payments
import bisqapps.shared.presentation.generated.resources.payment_icon_hal_cash
import bisqapps.shared.presentation.generated.resources.payment_icon_imps
import bisqapps.shared.presentation.generated.resources.payment_icon_interac_etransfer
import bisqapps.shared.presentation.generated.resources.payment_icon_mercado_pago
import bisqapps.shared.presentation.generated.resources.payment_icon_monese
import bisqapps.shared.presentation.generated.resources.payment_icon_money_beam
import bisqapps.shared.presentation.generated.resources.payment_icon_money_gram
import bisqapps.shared.presentation.generated.resources.payment_icon_national_bank_transfer
import bisqapps.shared.presentation.generated.resources.payment_icon_neft
import bisqapps.shared.presentation.generated.resources.payment_icon_pay_id
import bisqapps.shared.presentation.generated.resources.payment_icon_paysera
import bisqapps.shared.presentation.generated.resources.payment_icon_perfect_money
import bisqapps.shared.presentation.generated.resources.payment_icon_pin_4
import bisqapps.shared.presentation.generated.resources.payment_icon_pix
import bisqapps.shared.presentation.generated.resources.payment_icon_prompt_pay
import bisqapps.shared.presentation.generated.resources.payment_icon_revolut
import bisqapps.shared.presentation.generated.resources.payment_icon_same_bank
import bisqapps.shared.presentation.generated.resources.payment_icon_satispay
import bisqapps.shared.presentation.generated.resources.payment_icon_sbp
import bisqapps.shared.presentation.generated.resources.payment_icon_sepa
import bisqapps.shared.presentation.generated.resources.payment_icon_sepa_instant
import bisqapps.shared.presentation.generated.resources.payment_icon_strike
import bisqapps.shared.presentation.generated.resources.payment_icon_swift
import bisqapps.shared.presentation.generated.resources.payment_icon_swish
import bisqapps.shared.presentation.generated.resources.payment_icon_uphold
import bisqapps.shared.presentation.generated.resources.payment_icon_upi
import bisqapps.shared.presentation.generated.resources.payment_icon_uspmo
import bisqapps.shared.presentation.generated.resources.payment_icon_wechat
import bisqapps.shared.presentation.generated.resources.payment_icon_wise
import bisqapps.shared.presentation.generated.resources.payment_icon_wise_us
import bisqapps.shared.presentation.generated.resources.payment_icon_xmr
import bisqapps.shared.presentation.generated.resources.payment_icon_zelle
import org.jetbrains.compose.resources.DrawableResource

enum class PaymentMethodVO(
    val icon: DrawableResource? = null,
) {
    // Fiat
    ACH_TRANSFER(Res.drawable.payment_icon_ach),
    ADVANCED_CASH(Res.drawable.payment_icon_advanced_cash),
    ALI_PAY(Res.drawable.payment_icon_alipay),
    AMAZON_GIFT_CARD(Res.drawable.payment_icon_amazon),
    BIZUM(Res.drawable.payment_icon_bizum),
    CASH_APP(Res.drawable.payment_icon_cashapp),
    CASH_BY_MAIL(Res.drawable.payment_icon_cash_by_mail),
    CASH_DEPOSIT(Res.drawable.payment_icon_cash_deposit),
    CUSTOM,
    DOMESTIC_WIRE_TRANSFER(Res.drawable.payment_icon_domestic_wire_transfer),
    F2F(Res.drawable.payment_icon_f2f),
    FASTER_PAYMENTS(Res.drawable.payment_icon_faster_payments),
    HAL_CASH(Res.drawable.payment_icon_hal_cash),
    IMPS(Res.drawable.payment_icon_imps),
    INTERAC_E_TRANSFER(Res.drawable.payment_icon_interac_etransfer),
    MERCADO_PAGO(Res.drawable.payment_icon_mercado_pago),
    MONESE(Res.drawable.payment_icon_monese),
    MONEY_BEAM(Res.drawable.payment_icon_money_beam),
    MONEY_GRAM(Res.drawable.payment_icon_money_gram),
    NATIONAL_BANK(Res.drawable.payment_icon_national_bank_transfer),
    NEFT(Res.drawable.payment_icon_neft),
    PAY_ID(Res.drawable.payment_icon_pay_id),
    PAYSERA(Res.drawable.payment_icon_paysera),
    PERFECT_MONEY(Res.drawable.payment_icon_perfect_money),
    PIN_4(Res.drawable.payment_icon_pin_4),
    PIX(Res.drawable.payment_icon_pix),
    PROMPT_PAY(Res.drawable.payment_icon_prompt_pay),
    REVOLUT(Res.drawable.payment_icon_revolut),
    SAME_BANK(Res.drawable.payment_icon_same_bank),
    SATISPAY(Res.drawable.payment_icon_satispay),
    SBP(Res.drawable.payment_icon_sbp),
    SEPA(Res.drawable.payment_icon_sepa),
    SEPA_INSTANT(Res.drawable.payment_icon_sepa_instant),
    STRIKE(Res.drawable.payment_icon_strike),
    SWIFT(Res.drawable.payment_icon_swift),
    SWISH(Res.drawable.payment_icon_swish),
    UPHOLD(Res.drawable.payment_icon_uphold),
    UPI(Res.drawable.payment_icon_upi),
    US_POSTAL_MONEY_ORDER(Res.drawable.payment_icon_uspmo),
    WECHAT_PAY(Res.drawable.payment_icon_wechat),
    WISE(Res.drawable.payment_icon_wise),
    WISE_USD(Res.drawable.payment_icon_wise_us),
    ZELLE(Res.drawable.payment_icon_zelle),

    // Crypto
    XMR(Res.drawable.payment_icon_xmr),
    BSQ,
    LTC,
    ETH,
    ETC,
    LBTC,
    LNBTC,
    GRIN,
    ZEC,
    DOGE,
}

fun getPaymentMethodVOFromCryptoCurrencyCode(cryptoCurrencyCode: String): PaymentMethodVO? =
    when (cryptoCurrencyCode.trim().uppercase()) {
        "XMR" -> PaymentMethodVO.XMR
        "LTC" -> PaymentMethodVO.LTC
        "ETH" -> PaymentMethodVO.ETH
        "BSQ" -> PaymentMethodVO.BSQ
        "ETC" -> PaymentMethodVO.ETC
        "L-BTC" -> PaymentMethodVO.LBTC
        "LN-BTC" -> PaymentMethodVO.LNBTC
        "GRIN" -> PaymentMethodVO.GRIN
        "ZEC" -> PaymentMethodVO.ZEC
        "DOGE" -> PaymentMethodVO.DOGE
        else -> null
    }
