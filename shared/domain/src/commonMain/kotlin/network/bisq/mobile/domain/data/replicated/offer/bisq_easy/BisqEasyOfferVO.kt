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

) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BisqEasyOfferVO

        if (date != other.date) return false
        if (id != other.id) return false
        if (makerNetworkId != other.makerNetworkId) return false
        if (direction != other.direction) return false
        if (market != other.market) return false
        if (amountSpec != other.amountSpec) return false
        if (priceSpec != other.priceSpec) return false
        if (protocolTypes != other.protocolTypes) return false
        if (baseSidePaymentMethodSpecs != other.baseSidePaymentMethodSpecs) return false
        if (quoteSidePaymentMethodSpecs != other.quoteSidePaymentMethodSpecs) return false
        if (offerOptions != other.offerOptions) return false
        if (supportedLanguageCodes != other.supportedLanguageCodes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = date.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + makerNetworkId.hashCode()
        result = 31 * result + direction.hashCode()
        result = 31 * result + market.hashCode()
        result = 31 * result + amountSpec.hashCode()
        result = 31 * result + priceSpec.hashCode()
        result = 31 * result + protocolTypes.hashCode()
        result = 31 * result + baseSidePaymentMethodSpecs.hashCode()
        result = 31 * result + quoteSidePaymentMethodSpecs.hashCode()
        result = 31 * result + offerOptions.hashCode()
        result = 31 * result + supportedLanguageCodes.hashCode()
        return result
    }
}
