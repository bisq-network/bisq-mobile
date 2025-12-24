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
package network.bisq.mobile.domain.data.replicated.common.network

import kotlinx.serialization.Serializable

@Serializable
data class AddressVO(
    val host: String,
    val port: Int,
) {
    companion object {
        // Regex to parse URLs in formats: host:port, schema://host:port, schema://host
        private val URL_REGEX =
            Regex(
                """^(?:[a-zA-Z][a-zA-Z0-9+.-]*://)?([^/:]+)(?::(\d+))?(?:/.*)?$""",
            )

        fun from(url: String): AddressVO? {
            val trimmed = url.trim()
            if (trimmed.isBlank()) return null

            val match = URL_REGEX.matchEntire(trimmed) ?: return null
            val rawHost = match.groupValues[1]
            if (rawHost.isBlank()) return null

            val host = if (rawHost.endsWith(".onion")) rawHost.lowercase() else rawHost
            val port = match.groupValues[2].toIntOrNull() ?: return null
            if (port !in 1..65535) return null

            return AddressVO(host, port)
        }
    }

    override fun toString(): String = "$host:$port"
}
