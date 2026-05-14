package network.bisq.mobile.client.common.domain.utils

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import network.bisq.mobile.client.common.domain.httpclient.BisqProxyConfig

/**
 * Implementations of this function are expected to handle DNS leak by preventing system dns resolution in case proxy is a tor proxy.
 */
expect fun createHttpClient(
    host: String,
    tlsFingerprint: String?,
    proxyConfig: BisqProxyConfig? = null,
    config: HttpClientConfig<*>.() -> Unit = {},
): HttpClient

/**
 * Why this exists:
 *   Ktor's Darwin engine (3.4.3) closes its NSURLSession with `finishTasksAndInvalidate`
 *   which lets running tasks complete. For abandoned tasks blocked behind a
 *   stale Tor SOCKS proxy this can leave a zombie `NSURLSessionWebSocketTask`
 *   alive for up to 120s, polluting the connection pool of any successor
 *   `NSURLSession` (observed as `reused=1` in CFNetwork:Summary with
 *   `transaction_duration_ms` ~= 120000 and `Code=-1000 "bad URL"` on the next attempt).
 *
 *   This extension calls `invalidateAndCancel` on iOS,
 *   synchronously cancelling all tasks so the next [HttpClient] starts with a clean pool.
 *
 * Callers should invoke this before `HttpClient.close()` to short-circuit
 * any in-flight `webSocketSession { ... }` call that is still suspended inside a
 * `withTimeout(...)` block. The cancellation surfaces as an `NSURLErrorCancelled`
 * (`Code=-999`) into the awaiting coroutine, which then releases its
 * `connectionMutex` immediately.
 *
 * USE ONLY for forced/abnormal disposal (force-client-recreation, service deactivate,
 * test-connection cleanup). For routine settings-change replacement use
 * [releaseUnderlyingSessionTracking] so that an in-flight, healthy WebSocket can
 * still close its frame gracefully via the WebSocketClient's own dispose path.
 */
expect fun HttpClient.invalidateUnderlyingSession()

/**
 * Releases the platform-side bookkeeping for this client's underlying transport
 * WITHOUT cancelling its in-flight tasks.
 *
 * On iOS this drops the `HttpClient → NSURLSession` registry entry so it doesn't
 * leak across normal HttpClient recreations (e.g. when the bootstrap session-id
 * settings update triggers a new client). The NSURLSession itself is then closed
 * via Ktor's `HttpClient.close()` → `finishTasksAndInvalidate`, which lets any
 * in-flight WebSocket task finish its close-frame handshake instead of being
 * abruptly cancelled.
 */
expect fun HttpClient.releaseUnderlyingSessionTracking()
