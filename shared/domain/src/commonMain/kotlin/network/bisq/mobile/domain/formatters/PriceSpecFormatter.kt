package network.bisq.mobile.domain.formatters

import network.bisq.mobile.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.data.replicated.offer.price.spec.FloatPriceSpecVO
import network.bisq.mobile.data.replicated.offer.price.spec.PriceSpecVO
import network.bisq.mobile.i18n.i18n
import kotlin.math.abs

object PriceSpecFormatter {
    fun getFormattedPriceSpec(
        priceSpec: PriceSpecVO,
        abbreviated: Boolean = false,
    ): String =
        when (priceSpec) {
            is FixPriceSpecVO -> {
                val price = PriceFormatter.formatWithCode(priceSpec.priceQuote)
                "bisqEasy.tradeWizard.review.chatMessage.fixPrice".i18n(price)
            }

            is FloatPriceSpecVO -> {
                val percent = PercentageFormatter.format(abs(priceSpec.percentage))
                val key =
                    when {
                        priceSpec.percentage >= 0.0 ->
                            if (abbreviated) {
                                "bisqEasy.tradeWizard.review.chatMessage.floatPrice.plus"
                            } else {
                                "bisqEasy.tradeWizard.review.chatMessage.floatPrice.above"
                            }

                        else ->
                            if (abbreviated) {
                                "bisqEasy.tradeWizard.review.chatMessage.floatPrice.minus"
                            } else {
                                "bisqEasy.tradeWizard.review.chatMessage.floatPrice.below"
                            }
                    }
                key.i18n(percent)
            }

            else -> "bisqEasy.tradeWizard.review.chatMessage.marketPrice".i18n()
        }

    fun formatPriceWithSpec(
        formattedPrice: String,
        priceSpec: PriceSpecVO,
    ): String {
        if (priceSpec is FixPriceSpecVO) return formattedPrice
        val abbreviated = getFormattedPriceSpec(priceSpec, abbreviated = true)
        return "$formattedPrice ($abbreviated)"
    }

    /**
     * The price type on its own: "Fixed", "+2.00 % vs market", "-2.00 % vs market" or "Market price".
     * The sign is taken after rounding, so an offset that shows as 0.00 % reads "Market price".
     */
    fun formatPriceTypeLabel(priceSpec: PriceSpecVO): String =
        when (priceSpec) {
            is FixPriceSpecVO -> "mobile.bisqEasy.offerbook.offerCard.price.fixed".i18n()

            is FloatPriceSpecVO -> {
                val displayedPercent = PercentageFormatter.toDisplayedPercent(priceSpec.percentage)
                val percent = PercentageFormatter.format(abs(priceSpec.percentage))
                when {
                    displayedPercent > 0.0 -> "mobile.bisqEasy.offerbook.offerCard.price.floatAbove".i18n(percent)
                    displayedPercent < 0.0 -> "mobile.bisqEasy.offerbook.offerCard.price.floatBelow".i18n(percent)
                    else -> "bisqEasy.tradeWizard.review.chatMessage.marketPrice".i18n()
                }
            }

            else -> "bisqEasy.tradeWizard.review.chatMessage.marketPrice".i18n()
        }

    /** "50,000 USD (Fixed)": the price followed by [formatPriceTypeLabel], for every price type. */
    fun formatPriceWithTypeLabel(
        formattedPrice: String,
        priceSpec: PriceSpecVO,
    ): String = "$formattedPrice (${formatPriceTypeLabel(priceSpec)})"

    fun getFormattedPriceSpecWithOfferPrice(
        priceSpec: PriceSpecVO,
        offerPrice: String,
    ): String =
        when (priceSpec) {
            is FixPriceSpecVO -> {
                val price = PriceFormatter.formatWithCode(priceSpec.priceQuote)
                "priceSpecFormatter.fixPrice".i18n(price)
            }

            is FloatPriceSpecVO -> {
                val percent = PercentageFormatter.format(abs(priceSpec.percentage))
                val key =
                    if (priceSpec.percentage >= 0.0) {
                        "priceSpecFormatter.floatPrice.above"
                    } else {
                        "priceSpecFormatter.floatPrice.below"
                    }
                key.i18n(percent, offerPrice)
            }

            else -> "priceSpecFormatter.marketPrice".i18n(offerPrice)
        }
}
