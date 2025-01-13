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

object FiatVOFactory {
    fun FiatVOFactory.from(id: String, value: Long, code: String, precision: Int, lowPrecision: Int): FiatVO {
        return FiatVO(id, value, code, precision, lowPrecision)
    }

    fun FiatVOFactory.from(value: Long, code: String): FiatVO {
        return FiatVO(code, value, code, 4, 2)
    }

    fun FiatVOFactory.from(value: Long, code: String, precision: Int): FiatVO {
        return FiatVO(code, value, code, precision, 2)
    }

    fun FiatVOFactory.fromFaceValue(faceValue: Double, code: String): FiatVO {
        val value = FiatVOFactory.faceValueToLong(faceValue)
        return FiatVOFactory.from(value, code)
    }

    fun FiatVOFactory.faceValueToLong(faceValue: Double, precision: Int = 4): Long {
        val maxValue: Double = BigDecimal.fromLong(Long.MAX_VALUE).moveDecimalPoint(-precision).doubleValue(false)
        if (faceValue > maxValue) {
            throw RuntimeException("Provided value would lead to an overflow")
        }
        return BigDecimal.fromDouble(faceValue).moveDecimalPoint(precision).longValue(false)
    }
}

