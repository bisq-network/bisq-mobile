package network.bisq.mobile.domain.replicated.common.network

import kotlinx.serialization.Serializable

@Serializable
enum class TransportType {
    TOR,
    I2P,
    CLEAR;
}