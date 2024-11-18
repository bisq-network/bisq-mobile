package network.bisq.mobile.domain.network.identity

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.common.network.Address
import network.bisq.mobile.domain.common.network.TransportType
import network.bisq.mobile.domain.security.keys.PubKey

@Serializable
data class NetworkId(
    val addressByTransportTypeMap: Map<TransportType, Address>,
    val pubKey: PubKey,
)
