package network.bisq.mobile.domain.formatters

import network.bisq.mobile.domain.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.FloatPriceSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.PriceSpecVO
import network.bisq.mobile.i18n.i18n

object PriceSpecFormatter {

    fun getFormattedPriceSpec(priceSpec: PriceSpecVO, abbreviated: Boolean = false): String {
        return when (priceSpec) {
            is FixPriceSpecVO -> {
                val price = PriceFormatter.formatWithCode(priceSpec.priceQuote)
                "bisqEasy.tradeWizard.review.chatMessage.fixPrice".i18n(price)
            }

            is FloatPriceSpecVO -> {
                val percent = PercentageFormatter.format(kotlin.math.abs(priceSpec.percentage))
                val key = when {
                    priceSpec.percentage >= 0.0 -> if (abbreviated) "bisqEasy.tradeWizard.review.chatMessage.floatPrice.plus"
                    else "bisqEasy.tradeWizard.review.chatMessage.floatPrice.above"

                    else -> if (abbreviated) "bisqEasy.tradeWizard.review.chatMessage.floatPrice.minus"
                    else "bisqEasy.tradeWizard.review.chatMessage.floatPrice.below"
                }
                key.i18n(percent)
            }

            else -> "bisqEasy.tradeWizard.review.chatMessage.marketPrice".i18n()
        }
    }

    fun getFormattedPriceSpecWithOfferPrice(priceSpec: PriceSpecVO, offerPrice: String): String {
        return when (priceSpec) {
            is FixPriceSpecVO -> {
                val price = PriceFormatter.formatWithCode(priceSpec.priceQuote)
                "priceSpecFormatter.fixPrice".i18n(price)
            }

            is FloatPriceSpecVO -> {
                val percent = PercentageFormatter.format(kotlin.math.abs(priceSpec.percentage))
                val key = if (priceSpec.percentage >= 0.0) {
                    "priceSpecFormatter.floatPrice.above"
                } else {
                    "priceSpecFormatter.floatPrice.below"
                }
                key.i18n(percent, offerPrice)
            }

            else -> "priceSpecFormatter.marketPrice".i18n(offerPrice)
        }
    }
}