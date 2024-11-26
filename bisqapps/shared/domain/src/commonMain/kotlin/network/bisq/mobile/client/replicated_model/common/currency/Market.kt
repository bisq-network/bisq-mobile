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
package network.bisq.mobile.client.replicated_model.common.currency

import kotlinx.serialization.Serializable

@Serializable
class Market(
    val baseCurrencyCode: String,
    val quoteCurrencyCode: String,
    val baseCurrencyName: String,
    val quoteCurrencyName: String,
) {
    companion object {
        val EMPTY: Market = Market("", "", "", "")
        val USD: Market = Market("BTC", "USD", "Bitcoin", "US Dollar")
        private const val QUOTE_SEPARATOR = "/"
    }

    val marketCodes: String
        get() = baseCurrencyCode + QUOTE_SEPARATOR + quoteCurrencyCode

    override fun toString(): String {
        return "Market(baseCurrencyCode='$baseCurrencyCode', " +
                "quoteCurrencyCode='$quoteCurrencyCode', " +
                "baseCurrencyName='$baseCurrencyName', " +
                "quoteCurrencyName='$quoteCurrencyName'"
    }
}