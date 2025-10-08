package network.bisq.mobile.domain.service.offers

import network.bisq.mobile.domain.data.model.MarketPriceItem
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOExtensions.toBaseSideMonetary
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.QuoteSideFixedAmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.QuoteSideRangeAmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.PriceSpecVOExtensions.getPriceQuoteVO
import network.bisq.mobile.domain.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.domain.formatters.AmountFormatter
import network.bisq.mobile.domain.formatters.PriceQuoteFormatter

/**
 * Shared offer formatting utilities used by Node and Client facades to update
 * formatted price and base-amount strings when market prices tick.
 *
 * Behavior:
 * - Only offers whose price depends on market price (Market/Float) are updated
 * - Fixed-price offers are left unchanged
 * - On any error, previous formatted values are retained (callers should log if needed)
 */
object OfferFormattingUtil {
    fun updateOffersFormattedValues(
        offers: List<OfferItemPresentationModel>,
        marketItem: MarketPriceItem,
    ) {
        offers.forEach { model ->
            val offerVO = model.bisqEasyOffer
            val priceSpecVO = offerVO.priceSpec

            // Only offers depending on market price need updates
            if (priceSpecVO is FixPriceSpecVO) return@forEach

            // 1) Price
            runCatching {
                val priceQuoteVO = priceSpecVO.getPriceQuoteVO(marketItem)
                val newFormattedPrice = PriceQuoteFormatter.format(
                    priceQuoteVO,
                    useLowPrecision = true,
                    withCode = true
                )
                model.updateFormattedPrice(newFormattedPrice)
            }

            // 2) Base amount
            runCatching {
                val priceQuoteVO = priceSpecVO.getPriceQuoteVO(marketItem)
                val newFormattedBaseAmount = when (val amountSpec = offerVO.amountSpec) {
                    is QuoteSideFixedAmountSpecVO -> {
                        val quoteMonetary = FiatVOFactory.run {
                            from(amountSpec.amount, offerVO.market.quoteCurrencyCode)
                        }
                        val baseMonetary = priceQuoteVO.toBaseSideMonetary(quoteMonetary)
                        AmountFormatter.formatAmount(
                            baseMonetary,
                            useLowPrecision = false,
                            withCode = true
                        )
                    }

                    is QuoteSideRangeAmountSpecVO -> {
                        val minQuote = FiatVOFactory.run { from(amountSpec.minAmount, offerVO.market.quoteCurrencyCode) }
                        val maxQuote = FiatVOFactory.run { from(amountSpec.maxAmount, offerVO.market.quoteCurrencyCode) }
                        val minBase = priceQuoteVO.toBaseSideMonetary(minQuote)
                        val maxBase = priceQuoteVO.toBaseSideMonetary(maxQuote)
                        AmountFormatter.formatRangeAmount(
                            minBase,
                            maxBase,
                            useLowPrecision = false,
                            withCode = true
                        )
                    }

                    else -> model.formattedBaseAmount.value // Base-side specs unaffected by market price
                }
                model.updateFormattedBaseAmount(newFormattedBaseAmount)
            }
        }
    }
}

