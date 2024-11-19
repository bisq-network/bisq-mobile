package network.bisq.mobile.client.replicated_model.common.network

import kotlinx.serialization.Serializable

@Serializable
enum class TransportType {
    TOR,
    I2P,
    CLEAR;
}