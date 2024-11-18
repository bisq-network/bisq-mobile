package network.bisq.mobile.domain.common.network

import kotlinx.serialization.Serializable

@Serializable
data class Address(
    val host: String,
    val port: Int,
)