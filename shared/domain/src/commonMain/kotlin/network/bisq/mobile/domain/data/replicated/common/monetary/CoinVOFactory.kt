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

object CoinVOFactory {
    fun CoinVOFactory.from(id: String, value: Long, code: String, precision: Int, lowPrecision: Int): CoinVO {
        return CoinVO(id, value, code, precision, lowPrecision)
    }

    fun CoinVOFactory.bitcoinFrom(value: Long): CoinVO {
        return from(value, "BTC")
    }

    fun CoinVOFactory.from(value: Long, code: String): CoinVO {
        return CoinVO(code, value, code, 8, 4)
    }

    fun CoinVOFactory.from(value: Long, code: String, precision: Int): CoinVO {
        return CoinVO(code, value, code, precision, 4)
    }

    fun CoinVOFactory.fromFaceValue(faceValue: Double, code: String): CoinVO {
        val value = CoinVOFactory.faceValueToLong(faceValue)
        return CoinVOFactory.from(value, code)
    }

    fun CoinVOFactory.faceValueToLong(faceValue: Double, precision: Int = 8): Long {
        val maxValue: Double = BigDecimal.fromLong(Long.MAX_VALUE).moveDecimalPoint(-precision).doubleValue(false)
        if (faceValue > maxValue) {
            throw RuntimeException("Provided value would lead to an overflow")
        }
        return BigDecimal.fromDouble(faceValue).moveDecimalPoint(precision).longValue(false)
    }
}

