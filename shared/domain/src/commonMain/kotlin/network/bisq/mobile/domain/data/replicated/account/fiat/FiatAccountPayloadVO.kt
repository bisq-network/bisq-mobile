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
package network.bisq.mobile.domain.data.replicated.account.fiat

import kotlinx.serialization.Serializable

/**
 * Base interface for all fiat payment account payload VOs.
 * Each fiat payment rail type (CUSTOM, SEPA, REVOLUT, etc.) will have its own implementation.
 * Mirrors the Java FiatAccountPayloadDto sealed interface.
 *
 * Note: Payload type is automatically determined by the parent FiatAccountVO's concrete type.
 * No custom serializer needed - the parent's serializer handles the complete deserialization.
 */
@Serializable
sealed interface FiatAccountPayloadVO {
    // TODO: Add more implementations when needed:
    // - SepaAccountPayloadVO
    // - RevolutAccountPayloadVO
    // - ZelleAccountPayloadVO
    // - StrikeAccountPayloadVO
    // etc.
}
