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
import network.bisq.mobile.domain.data.replicated.common.monetary.CoinVOFactory.from
import network.bisq.mobile.domain.data.replicated.common.monetary.CoinVOFactory.fromFaceValue
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory.from
import network.bisq.mobile.domain.data.replicated.common.monetary.MonetaryVOExtensions.decimalMode

object PriceQuoteVOExtensions {
    // val PriceQuoteVO.baseSideMonetary: MonetaryVO get() = CoinVOFactory.fromFaceValue(1.0, market.baseCurrencyCode)
    // val PriceQuoteVO.quoteSideMonetary: MonetaryVO get() = FiatVOFactory.from(value, market.quoteCurrencyCode)

    fun PriceQuoteVO.toDouble(value: Long): Double {
        return BigDecimal.fromLong(value)
            .moveDecimalPoint(-quoteSideMonetary.precision)
            .scale(quoteSideMonetary.precision.toLong())
            .doubleValue(false)
    }

    fun PriceQuoteVO.asDouble(): Double {
        return toDouble(value)
    }

    fun PriceQuoteVO.toBaseSideMonetary(quoteSideMonetary: MonetaryVO): MonetaryVO {
        require(quoteSideMonetary::class == this.quoteSideMonetary::class) {
            "quoteSideMonetary must be the same type as the quote.quoteSideMonetary"
        }
        require(value != 0L) {
            "value must not be 0 as division by 0 is not allowed. PriceQuoteVO = $this"
        }

        val newValue: Long = BigDecimal.fromLong(quoteSideMonetary.value)
            .moveDecimalPoint(baseSideMonetary.precision)
            .divide(BigDecimal.fromLong(value), baseSideMonetary.decimalMode)
            .longValue(false)
        return if (baseSideMonetary is FiatVO) {
            FiatVOFactory.from(newValue, baseSideMonetary.code, baseSideMonetary.precision)
        } else {
            CoinVOFactory.from(newValue, baseSideMonetary.code, baseSideMonetary.precision)
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
        // TODO: PriceQuoteVO doesn't have baseSideMonetary as in bisq2 code,
        // but it's hardcoded to be CoinVO always, in this Extension
        // @Henrik: Is this right?
//        return if (this.baseSideMonetary is FiatVO) {
//            FiatVOFactory.from(value, this.baseSideMonetary.code, this.baseSideMonetary.precision)
//        } else {
//            CoinVOFactory.from(value, this.baseSideMonetary.code, this.baseSideMonetary.precision)
//        }

        return if (quoteSideMonetary is FiatVO) {
            FiatVOFactory.from(value, quoteSideMonetary.code, quoteSideMonetary.precision)
        } else {
            CoinVOFactory.from(value, quoteSideMonetary.code, quoteSideMonetary.precision)
        }
    }
}

