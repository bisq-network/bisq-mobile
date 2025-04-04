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
package network.bisq.mobile.domain.data.replicated.common.monetary

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.common.monetary.CoinVOFactory.from
import network.bisq.mobile.domain.data.replicated.common.monetary.CoinVOFactory.fromFaceValue
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory.from
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory.fromFaceValue
import network.bisq.mobile.domain.data.replicated.common.monetary.MonetaryVOExtensions.decimalMode

object PriceQuoteVOFactory {

    fun PriceQuoteVOFactory.fromPrice(priceValue: Double, market: MarketVO): PriceQuoteVO {
        return fromPrice(priceValue, market.baseCurrencyCode, market.quoteCurrencyCode)
    }

    fun PriceQuoteVOFactory.fromPrice(priceValue: Long, market: MarketVO): PriceQuoteVO {
        return fromPrice(priceValue, market.baseCurrencyCode, market.quoteCurrencyCode)
    }

    fun PriceQuoteVOFactory.fromPrice(priceValue: Double, baseCurrencyCode: String, quoteCurrencyCode: String): PriceQuoteVO {
        val baseSideMonetary: MonetaryVO =
            if (isFiat(baseCurrencyCode)) FiatVOFactory.fromFaceValue(1.0, baseCurrencyCode)
            else CoinVOFactory.fromFaceValue(1.0, baseCurrencyCode)
        val quoteSideMonetary: MonetaryVO =
            if (isFiat(quoteCurrencyCode)) FiatVOFactory.fromFaceValue(priceValue, quoteCurrencyCode)
            else CoinVOFactory.fromFaceValue(priceValue, quoteCurrencyCode)

        return from(baseSideMonetary, quoteSideMonetary)
    }

    fun PriceQuoteVOFactory.fromPrice(priceValue: Long, baseCurrencyCode: String, quoteCurrencyCode: String): PriceQuoteVO {
        val baseSideMonetary: MonetaryVO =
            if (isFiat(baseCurrencyCode)) FiatVOFactory.fromFaceValue(1.0, baseCurrencyCode)
            else CoinVOFactory.fromFaceValue(1.0, baseCurrencyCode)
        val quoteSideMonetary: MonetaryVO =
            if (isFiat(quoteCurrencyCode)) FiatVOFactory.from(priceValue, quoteCurrencyCode)
            else CoinVOFactory.from(priceValue, quoteCurrencyCode)

        return from(baseSideMonetary, quoteSideMonetary)
    }

    fun PriceQuoteVOFactory.from(baseSideMonetary: MonetaryVO, quoteSideMonetary: MonetaryVO): PriceQuoteVO {
        require(baseSideMonetary.value != 0L) { "baseSideMonetary.value must not be 0" }
        val value: Long = BigDecimal.fromLong(quoteSideMonetary.value)
            .moveDecimalPoint(baseSideMonetary.precision)
            .divide(BigDecimal.fromLong(baseSideMonetary.value), baseSideMonetary.decimalMode)
            .longValue(false)
        val marketVO = MarketVO(baseSideMonetary.code, quoteSideMonetary.code)
        return PriceQuoteVO(
            value,
            quoteSideMonetary.precision,
            quoteSideMonetary.lowPrecision,
            marketVO,
            baseSideMonetary,
            quoteSideMonetary
        )
    }

    private fun isFiat(code: String): Boolean {
        return !isCoin(code)
    }

    private fun isCoin(code: String): Boolean {
        return code == "BTC"
    }
}

