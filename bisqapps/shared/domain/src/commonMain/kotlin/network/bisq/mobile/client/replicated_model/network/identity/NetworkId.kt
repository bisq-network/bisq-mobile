package network.bisq.mobile.client.replicated_model.network.identity

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.replicated_model.common.network.Address
import network.bisq.mobile.client.replicated_model.common.network.TransportType
import network.bisq.mobile.client.replicated_model.security.keys.PubKey

@Serializable
data class NetworkId(
    val addressByTransportTypeMap: Map<TransportType, Address>,
    val pubKey: PubKey,
)
