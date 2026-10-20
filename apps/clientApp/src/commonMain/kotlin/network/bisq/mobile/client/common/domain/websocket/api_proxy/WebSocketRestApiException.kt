package network.bisq.mobile.client.common.domain.websocket.api_proxy

import io.ktor.http.HttpStatusCode
import network.bisq.mobile.domain.utils.LogRedactable

/**
 * [message] is the trusted node's response body (or its `error` field): it reaches the user, never a
 * log line. [errorCode] and [minRequiredVersion] are set when the node sends a structured error body,
 * for example for a take refused by a security-manager alert.
 */
class WebSocketRestApiException(
    val httpStatusCode: HttpStatusCode,
    message: String,
    val errorCode: String? = null,
    val minRequiredVersion: String? = null,
) : Exception(message),
    LogRedactable {
    override fun redactedDetails(): String = "http=${httpStatusCode.value}"
}
