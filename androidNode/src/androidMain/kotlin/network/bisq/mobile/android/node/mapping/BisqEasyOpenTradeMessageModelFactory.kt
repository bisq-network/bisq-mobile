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
package network.bisq.mobile.android.node.mapping

import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageDto
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageModel
import network.bisq.mobile.domain.data.replicated.chat.reactions.BisqEasyOpenTradeMessageReactionVO
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO


object BisqEasyOpenTradeMessageModelFactory {
    fun create(
        myUserProfile: UserProfileVO,
        bisqEasyOpenTradeMessageDto: BisqEasyOpenTradeMessageDto
    ): BisqEasyOpenTradeMessageModel {
        val chatMessageReactions: List<BisqEasyOpenTradeMessageReactionVO> = bisqEasyOpenTradeMessageDto.chatMessageReactions.toList()
        return BisqEasyOpenTradeMessageModel(
            bisqEasyOpenTradeMessageDto,
            myUserProfile,
            chatMessageReactions
        )
    }
}