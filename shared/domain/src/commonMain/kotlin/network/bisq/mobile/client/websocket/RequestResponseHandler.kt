package network.bisq.mobile.client.websocket

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import network.bisq.mobile.client.websocket.messages.WebSocketMessage
import network.bisq.mobile.client.websocket.messages.WebSocketRequest
import network.bisq.mobile.client.websocket.messages.WebSocketResponse
import network.bisq.mobile.domain.utils.Logging

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
        log.i { "Sending request with ID: ${webSocketRequest.requestId}" }
        mutex.withLock {
            require(requestId == null) { "RequestResponseHandler is designed to be used only once per request ID" }
            requestId = webSocketRequest.requestId
            deferredWebSocketResponse = CompletableDeferred()

            try {
                sendFunction.invoke(webSocketRequest)
            } catch (e: Exception) {
                deferredWebSocketResponse?.completeExceptionally(e)
                throw e
            }
        }
        return try {
            withTimeout(timeoutMillis) {
                deferredWebSocketResponse?.await()
            }
        } catch (e: TimeoutCancellationException) {
            log.w(e) { "WARN: Operation timed out after $timeoutMillis ms" }
            throw e
        }
    }

    suspend fun onWebSocketResponse(webSocketResponse: WebSocketResponse) {
        log.i { "Received response for request ID: ${webSocketResponse.requestId}" }
        mutex.withLock {
            require(webSocketResponse.requestId == requestId) { "Request ID of response does not match our request ID" }
            deferredWebSocketResponse?.complete(webSocketResponse)
        }
    }

    suspend fun dispose() {
        log.i { "Disposing request handler for ID: $requestId" }
        mutex.withLock {
            deferredWebSocketResponse?.cancel()
            deferredWebSocketResponse = null
            requestId = null
        }
    }
}