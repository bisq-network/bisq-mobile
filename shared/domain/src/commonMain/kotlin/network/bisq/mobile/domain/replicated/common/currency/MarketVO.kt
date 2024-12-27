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
package network.bisq.mobile.domain.replicated.common.currency

import kotlinx.serialization.Serializable

@Serializable
data class MarketVO(
    val baseCurrencyCode: String,
    val quoteCurrencyCode: String,
    val baseCurrencyName: String = baseCurrencyCode,
    val quoteCurrencyName: String = quoteCurrencyCode
)

val MarketVO.marketCodes: String
    get() = "$baseCurrencyCode/$quoteCurrencyCode"

object Markets {
    val EMPTY: MarketVO = MarketVO("", "")
    val USD: MarketVO = MarketVO("BTC", "USD", "Bitcoin", "US Dollar")
}