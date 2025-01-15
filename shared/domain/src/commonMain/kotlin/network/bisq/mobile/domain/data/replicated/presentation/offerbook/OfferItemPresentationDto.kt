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
package network.bisq.mobile.domain.data.replicated.presentation.offerbook

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO

/**
 * This value object is provided by the backend and is filled with presentation relevant data which are not trivial to provide by
 * the client code. It contains initial values for some mutual fields
 */
@Serializable
data class OfferItemPresentationDto(
    val bisqEasyOffer: BisqEasyOfferVO,
    val isMyOffer: Boolean,
    val userProfile: UserProfileVO,  // The userName inside userProfile can change when multiple nicknames are in the network
    val formattedDate: String,
    val formattedQuoteAmount: String,
    val formattedBaseAmount: String, // Can change by market price changes if float or market price is used
    val formattedPrice: String, // Can change by market price changes if float or market price is used
    val formattedPriceSpec: String,
    val quoteSidePaymentMethods: List<String>,
    val baseSidePaymentMethods: List<String>,
    val reputationScore: ReputationScoreVO // Can change by reputation changes
)
