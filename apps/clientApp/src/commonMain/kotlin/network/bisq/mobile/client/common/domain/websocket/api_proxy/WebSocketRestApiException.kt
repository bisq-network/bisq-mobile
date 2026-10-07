package network.bisq.mobile.client.common.domain.websocket.api_proxy

import io.ktor.http.HttpStatusCode
import network.bisq.mobile.domain.utils.LogRedactable

/** [message] is the trusted node's response body: it reaches the user, never a log line. */
class WebSocketRestApiException(
    val httpStatusCode: HttpStatusCode,
    message: String,
) : Exception(message),
    LogRedactable {
    override fun redactedDetails(): String = "http=${httpStatusCode.value}"
}
