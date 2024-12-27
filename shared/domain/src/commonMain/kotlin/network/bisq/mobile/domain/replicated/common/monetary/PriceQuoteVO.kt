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
package network.bisq.mobile.domain.replicated.common.monetary

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.replicated.common.currency.MarketVO


@Serializable
data class PriceQuoteVO(val value: Long, val market: MarketVO) {
}

val PriceQuoteVO.baseSideMonetary: MonetaryVO get() = CoinVO.fromFaceValue(1.0, market.baseCurrencyCode)
val PriceQuoteVO.quoteSideMonetary: MonetaryVO get() = FiatVO.from(value, market.quoteCurrencyCode)

fun PriceQuoteVO.toBaseSideMonetary(quoteSideMonetary: MonetaryVO): MonetaryVO {
    require(quoteSideMonetary::class == this.quoteSideMonetary::class) {
        "quoteSideMonetary must be the same type as the quote.quoteSideMonetary"
    }

    val value: Long = BigDecimal.fromLong(quoteSideMonetary.value)
        .moveDecimalPoint(baseSideMonetary.precision)
        .divide(BigDecimal.fromLong(value), baseSideMonetary.decimalMode)
        .longValue(false)
    return if (baseSideMonetary is FiatVO) {
        FiatVO.from(value, baseSideMonetary.code, baseSideMonetary.precision)
    } else {
        CoinVO.from(value, baseSideMonetary.code, baseSideMonetary.precision)
    }
}

fun PriceQuoteVO.toQuoteSideMonetary(baseSideMonetary: MonetaryVO): MonetaryVO {
    require(baseSideMonetary::class == this.baseSideMonetary::class) {
        "baseSideMonetary must be the same type as the quote.baseSideMonetary"
    }
    val value: Long = BigDecimal.fromLong(baseSideMonetary.value)
        .multiply(BigDecimal.fromLong(value), baseSideMonetary.decimalMode)
        .moveDecimalPoint(-baseSideMonetary.precision)
        .longValue(false)
    return if (this.baseSideMonetary is FiatVO) {
        FiatVO.from(value, this.baseSideMonetary.code, this.baseSideMonetary.precision)
    } else {
        CoinVO.from(value, this.baseSideMonetary.code, this.baseSideMonetary.precision)
    }
}

fun PriceQuoteVO.Companion.fromPrice(priceValue: Double, market: MarketVO): PriceQuoteVO {
    return fromPrice(priceValue, market.baseCurrencyCode, market.quoteCurrencyCode)
}

fun PriceQuoteVO.Companion.fromPrice(priceValue: Long, market: MarketVO): PriceQuoteVO {
    return fromPrice(priceValue, market.baseCurrencyCode, market.quoteCurrencyCode)
}

fun PriceQuoteVO.Companion.fromPrice(priceValue: Double, baseCurrencyCode: String, quoteCurrencyCode: String): PriceQuoteVO {
    val baseSideMonetary: MonetaryVO =
        if (isFiat(baseCurrencyCode)) FiatVO.fromFaceValue(1.0, baseCurrencyCode)
        else CoinVO.fromFaceValue(1.0, baseCurrencyCode)
    val quoteSideMonetary: MonetaryVO =
        if (isFiat(quoteCurrencyCode)) FiatVO.fromFaceValue(priceValue, quoteCurrencyCode)
        else CoinVO.fromFaceValue(priceValue, quoteCurrencyCode)

    return from(baseSideMonetary, quoteSideMonetary)
}

fun PriceQuoteVO.Companion.fromPrice(priceValue: Long, baseCurrencyCode: String, quoteCurrencyCode: String): PriceQuoteVO {
    val baseSideMonetary: MonetaryVO =
        if (isFiat(baseCurrencyCode)) FiatVO.fromFaceValue(1.0, baseCurrencyCode)
        else CoinVO.fromFaceValue(1.0, baseCurrencyCode)
    val quoteSideMonetary: MonetaryVO =
        if (isFiat(quoteCurrencyCode)) FiatVO.from(priceValue, quoteCurrencyCode)
        else CoinVO.from(priceValue, quoteCurrencyCode)

    return from(baseSideMonetary, quoteSideMonetary)
}

fun PriceQuoteVO.Companion.from(baseSideMonetary: MonetaryVO, quoteSideMonetary: MonetaryVO): PriceQuoteVO {
    require(baseSideMonetary.value != 0L) { "baseSideMonetary.value must not be 0" }
    val value: Long = BigDecimal.fromLong(quoteSideMonetary.value)
        .moveDecimalPoint(baseSideMonetary.precision)
        .divide(BigDecimal.fromLong(baseSideMonetary.value), baseSideMonetary.decimalMode)
        .longValue(false)
    val marketVO = MarketVO(baseSideMonetary.code, quoteSideMonetary.code)
    return PriceQuoteVO(value, marketVO)
}

fun PriceQuoteVO.toDouble(value: Long): Double {
    return BigDecimal.fromLong(value)
        .moveDecimalPoint(-quoteSideMonetary.precision)
        .scale(quoteSideMonetary.precision.toLong())
        .doubleValue(false)
}

fun PriceQuoteVO.asDouble(): Double {
    return toDouble(value)
}
