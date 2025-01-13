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
import com.ionspin.kotlin.bignum.decimal.DecimalMode
import com.ionspin.kotlin.bignum.decimal.RoundingMode

object MonetaryVOExtensions {
    fun MonetaryVO.toDouble(value: Long): Double {
        return BigDecimal.fromLong(value).moveDecimalPoint(-precision).scale(precision.toLong()).doubleValue(false)
    }

    fun MonetaryVO.asDouble(): Double {
        return toDouble(value)
    }

    val MonetaryVO.decimalMode: DecimalMode get() = DecimalMode(precision.toLong(), RoundingMode.ROUND_HALF_AWAY_FROM_ZERO)
}

