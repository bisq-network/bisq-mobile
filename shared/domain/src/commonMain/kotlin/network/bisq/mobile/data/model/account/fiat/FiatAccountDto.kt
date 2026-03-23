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
package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

/**
 * Base interface for all fiat payment account VOs.
 * Each fiat payment rail type (CUSTOM, SEPA, REVOLUT, etc.) will have its own implementation.
 * All fiat account VOs must have an account name and payload.
 * Mirrors the Java FiatAccountDto sealed interface.
 *
 * Uses custom serializer for content-based polymorphic deserialization.
 */
@Serializable(with = FiatAccountDtoSerializer::class)
sealed interface FiatAccountDto {
    val accountName: String
    val accountPayload: FiatAccountPayloadDto
    val paymentRail: FiatPaymentRailDto
}
