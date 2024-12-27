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
package network.bisq.mobile.domain.replicated.offer.bisq_easy

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.replicated.user.reputation.ReputationScoreVO

@Serializable
data class OfferListItemVO(
    val bisqEasyOffer: BisqEasyOfferVO,
    val isMyOffer: Boolean,
    val nym: String,
    val userName: String,
    val reputationScore: ReputationScoreVO,
    val formattedDate: String,
    val formattedQuoteAmount: String,
    val formattedBaseAmount: String,
    val formattedPrice: String,
    val formattedPriceSpec: String,
    val quoteSidePaymentMethods: List<String>,
    val baseSidePaymentMethods: List<String>,
)
