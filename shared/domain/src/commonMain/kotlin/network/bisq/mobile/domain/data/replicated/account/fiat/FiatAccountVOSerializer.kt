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

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import network.bisq.mobile.domain.data.replicated.account.payment_method.FiatPaymentRailEnum

/**
 * Custom serializer for FiatAccountVO that uses content-based polymorphic deserialization.
 * Determines the concrete type based on the 'paymentRail' field in the JSON.
 */
object FiatAccountVOSerializer : JsonContentPolymorphicSerializer<FiatAccountVO>(FiatAccountVO::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<FiatAccountVO> {
        val paymentRailValue =
            element.jsonObject["paymentRail"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("Missing 'paymentRail' field in FiatAccountVO JSON")

        return when (paymentRailValue) {
            FiatPaymentRailEnum.CUSTOM.name -> UserDefinedFiatAccountVO.serializer()
            // TODO: Add more cases when implemented:
            // FiatPaymentRailEnum.SEPA.name -> SepaAccountVO.serializer()
            // FiatPaymentRailEnum.REVOLUT.name -> RevolutAccountVO.serializer()
            // FiatPaymentRailEnum.ZELLE.name -> ZelleAccountVO.serializer()
            // FiatPaymentRailEnum.STRIKE.name -> StrikeAccountVO.serializer()
            // etc.
            else -> throw IllegalArgumentException(
                "Unsupported or not yet implemented payment rail: $paymentRailValue. " +
                    "Only CUSTOM is currently supported.",
            )
        }
    }
}
