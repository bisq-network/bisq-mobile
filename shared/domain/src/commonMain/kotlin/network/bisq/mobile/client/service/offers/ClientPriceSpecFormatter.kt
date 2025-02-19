package network.bisq.mobile.client.service.offers

/*
class ClientPriceSpecFormatter {
    companion object {
        fun getFormattedPriceSpec(priceSpec: PriceSpec, abbreviated: Boolean): String {
            return when (priceSpec) {
                is FixPriceSpec -> {
                    val price = PriceFormatter.formatWithCode(priceSpec.priceQuote)
                    "bisqEasy.tradeWizard.review.chatMessage.fixPrice".i18n(price)
                }
                is FloatPriceSpec -> {
                    val percent = PercentageFormatter.formatToPercentWithSymbol(priceSpec.percentage.absoluteValue)
                    val key = when {
                        priceSpec.percentage >= 0.0 -> if (abbreviated) "bisqEasy.tradeWizard.review.chatMessage.floatPrice.plus" else "bisqEasy.tradeWizard.review.chatMessage.floatPrice.above"
                        else -> if (abbreviated) "bisqEasy.tradeWizard.review.chatMessage.floatPrice.minus" else "bisqEasy.tradeWizard.review.chatMessage.floatPrice.below"
                    }
                    key.i18n(percent)
                }
                else -> "bisqEasy.tradeWizard.review.chatMessage.marketPrice".i18n()
            }
        }
    }
}
*/