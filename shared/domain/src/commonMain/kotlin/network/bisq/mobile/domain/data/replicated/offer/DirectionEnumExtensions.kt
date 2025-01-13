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
package network.bisq.mobile.domain.data.replicated.offer

import kotlinx.serialization.Serializable

@Serializable
object DirectionEnumExtensions {
    val DirectionEnum.isBuy: Boolean
        get() {
            return this == DirectionEnum.BUY
        }

    val DirectionEnum.isSell: Boolean
        get() {
            return this == DirectionEnum.SELL
        }

    val DirectionEnum.mirror get(): DirectionEnum = if (isBuy) DirectionEnum.SELL else DirectionEnum.BUY

    val DirectionEnum.displayString get(): String = if (isBuy) "Buy" else "Sell" // todo use i18n
}