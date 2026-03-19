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
package network.bisq.mobile.data.model.offerbook

import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.utils.CurrencyUtils

/**
 * Provides data for offerbook market list items
 */
data class MarketListItem(
    val market: MarketVO,
    val numOffers: Int,
    val localeFiatCurrencyName: String,
    val localizedCurrencyLocaleTag: String = "",
) {
    companion object {
        fun from(
            market: MarketVO,
            numOffers: Int = 0,
            languageCode: String = "",
        ): MarketListItem =
            MarketListItem(
                market,
                numOffers,
                CurrencyUtils.getLocaleFiatCurrencyName(
                    market.quoteCurrencyCode,
                    market.quoteCurrencyName,
                ),
                localizedCurrencyLocaleTag = languageCode,
            )
    }

    fun withLocaleFiatCurrencyName(languageCode: String): MarketListItem {
        if (localizedCurrencyLocaleTag == languageCode && localeFiatCurrencyName.isNotBlank()) {
            return this
        }

        return copy(
            localeFiatCurrencyName =
                CurrencyUtils.getLocaleFiatCurrencyName(
                    market.quoteCurrencyCode,
                    market.quoteCurrencyName,
                ),
            localizedCurrencyLocaleTag = languageCode,
        )
    }

    override fun toString(): String =
        "MarketListItem(market=$market, " +
            "numOffers=$numOffers, " +
            "localeFiatCurrencyName=$localeFiatCurrencyName)"
}

fun List<MarketListItem>.withLocaleFiatCurrencyNames(languageCode: String): List<MarketListItem> {
    var localizedItems: ArrayList<MarketListItem>? = null

    // Reuse the original list unless at least one item needs a locale-specific update.
    for (index in indices) {
        val item = this[index]
        val localizedItem = item.withLocaleFiatCurrencyName(languageCode)

        if (localizedItems != null) {
            localizedItems.add(localizedItem)
        } else if (localizedItem !== item) {
            localizedItems = ArrayList(size)
            for (existingIndex in 0 until index) {
                localizedItems.add(this[existingIndex])
            }
            localizedItems.add(localizedItem)
        }
    }

    return localizedItems ?: this
}
