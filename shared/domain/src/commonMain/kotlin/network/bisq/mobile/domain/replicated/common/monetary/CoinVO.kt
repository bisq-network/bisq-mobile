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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Coin")
data class CoinVO(
    override val id: String,
    override val value: Long,
    override val code: String,
    override val precision: Int,
    override val lowPrecision: Int,
) : MonetaryVO


fun CoinVO.Companion.from(id: String, value: Long, code: String, precision: Int, lowPrecision: Int): CoinVO {
    return CoinVO(id, value, code, precision, lowPrecision)
}

fun CoinVO.Companion.from(value: Long, code: String): CoinVO {
    return CoinVO(code, value, code, 8, 4)
}

fun CoinVO.Companion.from(value: Long, code: String, precision: Int): CoinVO {
    return CoinVO(code, value, code, precision, 4)
}

fun CoinVO.Companion.fromFaceValue(faceValue: Double, code: String): CoinVO {
    val value = faceValueToLong(faceValue, 8)
    return CoinVO.from(value, code)
}