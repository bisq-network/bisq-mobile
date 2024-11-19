package network.bisq.mobile.client.replicated_model.common.network

import kotlinx.serialization.Serializable

@Serializable
data class Address(
    val host: String,
    val port: Int,
)