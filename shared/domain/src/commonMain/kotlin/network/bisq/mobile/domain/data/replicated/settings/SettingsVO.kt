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
package network.bisq.mobile.domain.data.replicated.settings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.i18n.DEFAULT_LANGUAGE_CODE

const val DEFAULT_MAX_TRADE_PRICE_DEVIATION = 0.1
const val DEFAULT_NUM_DAYS_AFTER_REDACTING_TRADE_DATA = 90

@Serializable
data class SettingsVO(
    val isTacAccepted: Boolean = false,
    @SerialName("bisqEasyTradeRulesConfirmed")
    val tradeRulesConfirmed: Boolean = false,
    val closeMyOfferWhenTaken: Boolean = false,
    val languageCode: String = DEFAULT_LANGUAGE_CODE,
    val supportedLanguageCodes: Set<String> = emptySet(),
    val maxTradePriceDeviation: Double = DEFAULT_MAX_TRADE_PRICE_DEVIATION,
    val useAnimations: Boolean = true,
    val selectedMarket: MarketVO? = null,
    val numDaysAfterRedactingTradeData: Int = DEFAULT_NUM_DAYS_AFTER_REDACTING_TRADE_DATA,
)

val settingsVODemoObj =
    SettingsVO(
        isTacAccepted = true,
        tradeRulesConfirmed = true,
        closeMyOfferWhenTaken = true,
        languageCode = "en",
        supportedLanguageCodes = setOf("en", "es"),
        maxTradePriceDeviation = 1.0,
        useAnimations = true,
        selectedMarket = MarketVO("AUD", "AUD"),
        numDaysAfterRedactingTradeData = 1,
    )
