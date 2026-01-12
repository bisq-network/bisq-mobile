package network.bisq.mobile.client.common.domain.websocket.messages

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.utils.createUuid

@Serializable
@SerialName("WebSocketRestApiRequest")
data class WebSocketRestApiRequest(
    override val requestId: String,
    val method: String,
    val path: String,
    val body: String,

    // TODO
    // Device UUID is created and persisted at the client. It will be associated with a user defined device
    // name which will be sent to the server in the PairingRequestPayload.
    val deviceId: String = createUuid(),
) : WebSocketRequest
