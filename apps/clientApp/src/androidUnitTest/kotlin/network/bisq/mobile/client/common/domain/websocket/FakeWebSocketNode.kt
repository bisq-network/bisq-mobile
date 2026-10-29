package network.bisq.mobile.client.common.domain.websocket

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.access.utils.Headers
import network.bisq.mobile.client.common.domain.websocket.messages.SubscriptionRequest
import network.bisq.mobile.client.common.domain.websocket.messages.SubscriptionResponse
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketEvent
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketMessage
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketRestApiRequest
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketRestApiResponse

/**
 * A node behind a fake WS session for [WebSocketClientImpl] tests. The test must
 * `mockkStatic("io.ktor.client.plugins.websocket.BuildersKt")` so [httpClient] can open the session.
 *
 * Every REST request proxied over the socket is answered from [responses] (path -> status and body;
 * unknown paths get 404). Requested paths are recorded in [requestedPaths] and the headers of each
 * request in [requestedHeaders]. Paths in [credentialsReadFromMessage] answer 401 unless the message
 * carries the session and client headers, as a node before bisq-api 2.1.14 does.
 *
 * A [SubscriptionRequest] is recorded in [subscriptionRequests] and answered with an empty snapshot, or
 * with [subscriptionError], after the events of [eventsBeforeSubscriptionResponse]: like bisq2, the node
 * can push events of a subscriber before its response. With [answerSubscriptions] false it stays unanswered.
 */
class FakeWebSocketNode(
    private val json: Json,
    private val responses: Map<String, Pair<Int, String>> = emptyMap(),
    private val credentialsReadFromMessage: Set<String> = emptySet(),
) {
    val requestedPaths = mutableListOf<String>()
    val requestedHeaders = mutableListOf<Map<String, String>>()
    val subscriptionRequests = mutableListOf<SubscriptionRequest>()
    var eventsBeforeSubscriptionResponse: (subscriberId: String) -> List<WebSocketEvent> = { emptyList() }
    var subscriptionError: String? = null
    var answerSubscriptions = true
    val httpClient = mockk<HttpClient>()

    /** Frames of the current session; closing it simulates a server-side close. */
    lateinit var incoming: Channel<Frame>
        private set

    init {
        coEvery { httpClient.webSocketSession(any<HttpRequestBuilder.() -> Unit>()) } answers { newSession() }
    }

    /** Pushes [message] to the client, as the node does on its own (events, a late response). */
    suspend fun push(message: WebSocketMessage) {
        incoming.send(Frame.Text(json.encodeToString(WebSocketMessage.serializer(), message)))
    }

    private fun newSession(): DefaultClientWebSocketSession {
        val channel = Channel<Frame>(Channel.UNLIMITED)
        incoming = channel
        val session = mockk<DefaultClientWebSocketSession>(relaxed = true)
        every { session.coroutineContext } returns Job()
        every { session.incoming } returns channel
        coEvery { session.send(any()) } coAnswers {
            val frame = firstArg<Frame>()
            if (frame is Frame.Text) {
                when (val request = json.decodeFromString(WebSocketMessage.serializer(), frame.readText())) {
                    is WebSocketRestApiRequest -> push(answer(request))
                    is SubscriptionRequest -> answer(request)
                    else -> error("Unexpected request $request")
                }
            } else {
                channel.close()
            }
        }
        return session
    }

    private fun answer(request: WebSocketRestApiRequest): WebSocketRestApiResponse {
        requestedPaths += request.path
        requestedHeaders += request.headers
        val hasCredentials = Headers.SESSION_ID in request.headers && Headers.CLIENT_ID in request.headers
        val (status, body) =
            if (request.path in credentialsReadFromMessage && !hasCredentials) {
                401 to ""
            } else {
                responses[request.path] ?: (404 to "")
            }
        return WebSocketRestApiResponse(request.requestId, status, body)
    }

    private suspend fun answer(request: SubscriptionRequest) {
        subscriptionRequests += request
        if (!answerSubscriptions) return
        eventsBeforeSubscriptionResponse(request.requestId).forEach { push(it) }
        val error = subscriptionError
        push(SubscriptionResponse(request.requestId, payload = if (error == null) "[]" else null, errorMessage = error))
    }
}
