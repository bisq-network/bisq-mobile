/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation either version 3 of the License or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not see <http://www.gnu.org/licenses/>.
 */
package network.bisq.mobile.domain.data.replicated.common.monetary

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import network.bisq.mobile.domain.data.replicated.common.monetary.MonetaryVOExtensions.toDouble
import network.bisq.mobile.domain.data.replicated.common.roundDouble
import network.bisq.mobile.domain.data.replicated.common.scaleDownByPowerOf10
import kotlin.math.pow
import kotlin.math.round

sealed interface MonetaryVO {
    val id: String
    val value: Long
    val code: String
    val precision: Int
    val lowPrecision: Int

    companion object {
        fun toFaceValue(value: MonetaryVO, _precision: Int): Double {
            return value.toFaceValue(_precision)
        }
    }
    fun toFaceValue(_precision: Int): Double {
        val fullPrecision = scaleDownByPowerOf10(value, precision)
        return roundDouble(fullPrecision, _precision)
    }


    fun round(roundPrecision: Int): MonetaryVO

    fun toDouble(): Double {
        return BigDecimal.fromLong(value).moveDecimalPoint(-precision).doubleValue(false)
    }

}