package network.bisq.mobile.client.websocket

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import network.bisq.mobile.client.websocket.messages.WebSocketMessage
import network.bisq.mobile.client.websocket.messages.WebSocketRequest
import network.bisq.mobile.client.websocket.messages.WebSocketResponse
import network.bisq.mobile.utils.Logging

/**
 * Handles request-response communication over a WebSocket connection.
 *
 * This class is designed to manage the lifecycle of a WebSocket request and its corresponding response
 * using a unique request ID for correlation. It provides thread-safe mechanisms to send a request,
 * await its response, and handle WebSocket responses asynchronously. The class also supports cleanup
 * of ongoing requests if necessary. It is designed to be used only once per request ID.
 *
 * @param sendFunction A suspending function used to send WebSocket messages.
 *                     It takes a [WebSocketMessage] as input and sends it over the WebSocket connection.
 */
class RequestResponseHandler(private val sendFunction: suspend (WebSocketMessage) -> Unit) :
    Logging {
    private var requestId: String? = null
    private var deferredWebSocketResponse: CompletableDeferred<WebSocketResponse>? = null
    private val mutex = Mutex()

    suspend fun request(
        webSocketRequest: WebSocketRequest,
        timeoutMillis: Long = 10_000
    ): WebSocketResponse? {
        require(requestId == null) { "RequestResponseHandler is designed to be used only once per request ID" }
        log.i { "Sending request with ID: ${webSocketRequest.requestId}" }
        requestId = webSocketRequest.requestId
        mutex.withLock { deferredWebSocketResponse = CompletableDeferred() }

        try {
            sendFunction.invoke(webSocketRequest)
        } catch (e: Exception) {
            mutex.withLock { deferredWebSocketResponse?.completeExceptionally(e) }
            throw e
        }

        return withTimeout(timeoutMillis) {
            deferredWebSocketResponse?.await()
        }
    }

    suspend fun onWebSocketResponse(webSocketResponse: WebSocketResponse) {
        require(webSocketResponse.requestId == requestId) { "Request ID of response does not match our request ID" }
        log.i { "Received response for request ID: ${webSocketResponse.requestId}" }
        mutex.withLock { deferredWebSocketResponse?.complete(webSocketResponse) }
    }

    suspend fun dispose() {
        log.i { "Disposing request handler for ID: $requestId" }
        mutex.withLock {
            deferredWebSocketResponse?.cancel()
            deferredWebSocketResponse = null
        }
        requestId = null
    }
}