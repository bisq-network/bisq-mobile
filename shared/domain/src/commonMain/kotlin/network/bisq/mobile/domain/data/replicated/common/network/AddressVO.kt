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

import io.ktor.http.parseUrl
import kotlinx.serialization.Serializable

@Serializable
data class AddressVO(val host: String, val port: Int) {
    companion object {
        fun from(url: String): AddressVO? {
            var url = url.trim()
            if (url.isBlank()) return null
            if (!url.contains("://")) {
                // hack to make it parsable if it doesn't contain schema
                url = "schema://$url"
            }
            val parsed = parseUrl(url)
            if (parsed == null) return null
            val rawHost = parsed.host
            if (rawHost.isBlank()) return null
            val host = if (rawHost.endsWith(".onion")) rawHost.lowercase() else rawHost
            val port = parsed.port
            if (port !in 1..65535) return null
            return AddressVO(host, port)
        }
    }
}
