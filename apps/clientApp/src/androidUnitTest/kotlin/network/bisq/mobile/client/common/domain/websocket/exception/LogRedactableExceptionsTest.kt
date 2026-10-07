package network.bisq.mobile.client.common.domain.websocket.exception

import io.ktor.http.HttpStatusCode
import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketRestApiException
import network.bisq.mobile.domain.utils.redactedSummary
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** The client exceptions that opt into log details expose counts and status codes, never their message. */
class LogRedactableExceptionsTest {
    @Test
    fun `MaximumRetryReachedException reports the attempt count`() {
        assertEquals("MaximumRetryReachedException(attempts=3)", MaximumRetryReachedException(3).redactedSummary())
    }

    @Test
    fun `MaximumRetryReachedException keeps its cause chain in the summary`() {
        val error = MaximumRetryReachedException(5, IllegalStateException("abc.onion refused"))

        val summary = error.redactedSummary()

        assertEquals("MaximumRetryReachedException(attempts=5) <- IllegalStateException", summary)
        assertFalse(summary.contains("onion"), summary)
    }

    @Test
    fun `WebSocketRestApiException reports the http status and not the body`() {
        val error = WebSocketRestApiException(HttpStatusCode.NotFound, "Trade not found for ID 7b2e-peer")

        val summary = error.redactedSummary()

        assertEquals("WebSocketRestApiException(http=404)", summary)
        assertFalse(summary.contains("7b2e"), summary)
    }
}
