/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */
package network.bisq.mobile.domain.replicated.offer.price.spec

import network.bisq.mobile.domain.data.model.MarketPriceItem
import network.bisq.mobile.domain.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.domain.replicated.common.monetary.fromPrice
import kotlin.math.roundToLong

sealed interface PriceSpecVO {
}

fun PriceSpecVO.getPriceQuoteVO(marketPriceItem: MarketPriceItem): PriceQuoteVO {
    if (this is FixPriceSpecVO) {
        return this.priceQuote
    } else if (this is FloatPriceSpecVO) {
        val floatPricePercentage: Double = this.percentage
        val adjustedPrice = marketPriceItem.priceQuote.value * (1 + floatPricePercentage)
        val priceValue = adjustedPrice.roundToLong()
        return PriceQuoteVO.fromPrice(
            priceValue,
            marketPriceItem.market.baseCurrencyCode,
            marketPriceItem.market.quoteCurrencyCode
        )
    } else {
        // MarketPriceSpec
        return marketPriceItem.priceQuote
    }
}