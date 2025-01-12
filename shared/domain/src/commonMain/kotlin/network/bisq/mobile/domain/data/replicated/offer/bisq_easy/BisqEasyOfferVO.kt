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
package network.bisq.mobile.domain.data.replicated.offer.bisq_easy

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.data.replicated.account.protocol_type.TradeProtocolTypeEnum
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.AmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.options.OfferOptionVO
import network.bisq.mobile.domain.data.replicated.offer.payment_method.BitcoinPaymentMethodSpecVO
import network.bisq.mobile.domain.data.replicated.offer.payment_method.FiatPaymentMethodSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.PriceSpecVO

@Serializable
data class BisqEasyOfferVO(
    val id: String,
    val date: Long,
    val makerNetworkId: NetworkIdVO,
    val direction: DirectionEnum,
    val market: MarketVO,
    val amountSpec: AmountSpecVO,
    val priceSpec: PriceSpecVO,
    val protocolTypes: List<TradeProtocolTypeEnum>,
    val baseSidePaymentMethodSpecs: List<BitcoinPaymentMethodSpecVO>,
    val quoteSidePaymentMethodSpecs: List<FiatPaymentMethodSpecVO>,
    val offerOptions: List<OfferOptionVO>,
    val supportedLanguageCodes: List<String>
)
