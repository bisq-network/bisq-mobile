package network.bisq.mobile.client.websocket

sealed class ConnectionState {
    data class Disconnected(val error: Throwable? = null) : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
}