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
package network.bisq.mobile.domain.data.replicated.chat.bisq_easy.offerbook

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.data.replicated.chat.ChatMessageTypeEnum
import network.bisq.mobile.domain.data.replicated.chat.CitationVO
import network.bisq.mobile.domain.data.replicated.offer.bisq_easy.BisqEasyOfferVO

@Serializable
data class BisqEasyOfferbookMessageDto(
    val id: String,
    val channelId: String,
    val authorUserProfileId: String,
    val bisqEasyOffer: BisqEasyOfferVO?,
    val text: String?,
    val citation: CitationVO?,
    val date: Long,
    val wasEdited: Boolean,
    val chatMessageType: ChatMessageTypeEnum
)
