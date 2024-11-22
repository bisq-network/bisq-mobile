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
package network.bisq.mobile.android.node.domain.offerbook

import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannel
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookMessage
import bisq.common.observable.Pin
import lombok.EqualsAndHashCode
import lombok.Getter
import lombok.extern.slf4j.Slf4j

@Slf4j
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
class NumOffersObserver(
    private val channel: BisqEasyOfferbookChannel,
    val setNumOffers: (Int) -> Unit
) {
    private var channelPin: Pin? = null

    init {
        channelPin = channel.chatMessages.addObserver { this.updateNumOffers() }
    }

    fun resume() {
        dispose()
        channelPin = channel.chatMessages.addObserver { this.updateNumOffers() }
    }

    fun dispose() {
        channelPin?.unbind()
        channelPin = null
    }

    private fun updateNumOffers() {
        val numOffers = channel.chatMessages.stream()
            .filter { obj: BisqEasyOfferbookMessage -> obj.hasBisqEasyOffer() }
            .count().toInt()
        setNumOffers(numOffers)
    }
}
