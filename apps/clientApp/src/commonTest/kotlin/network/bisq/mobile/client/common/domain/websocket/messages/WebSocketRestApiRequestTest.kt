package network.bisq.mobile.client.common.domain.websocket.messages

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WebSocketRestApiRequestTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `properties are accessible`() {
        val request =
            WebSocketRestApiRequest(
                requestId = "req-123",
                method = "GET",
                path = "/api/v1/test",
                body = "",
            )

        assertEquals("req-123", request.requestId)
        assertEquals("GET", request.method)
        assertEquals("/api/v1/test", request.path)
        assertEquals("", request.body)
        assertTrue(request.headers.isEmpty())
    }

    @Test
    fun `headers can be set`() {
        val headers = mapOf("Content-Type" to "application/json", "Authorization" to "Bearer token")
        val request =
            WebSocketRestApiRequest(
                requestId = "req-123",
                method = "POST",
                path = "/api/v1/test",
                body = """{"key": "value"}""",
                headers = headers,
            )

        assertEquals(2, request.headers.size)
        assertEquals("application/json", request.headers["Content-Type"])
        assertEquals("Bearer token", request.headers["Authorization"])
    }

    @Test
    fun `serialization works`() {
        val request =
            WebSocketRestApiRequest(
                requestId = "req-123",
                method = "GET",
                path = "/api/v1/test",
                body = "",
            )

        val jsonString = json.encodeToString(request)
        val decoded = json.decodeFromString<WebSocketRestApiRequest>(jsonString)

        assertEquals(request, decoded)
    }

    @Test
    fun `serialization with headers works`() {
        val request =
            WebSocketRestApiRequest(
                requestId = "req-123",
                method = "POST",
                path = "/api/v1/test",
                body = """{"data": "test"}""",
                headers = mapOf("X-Custom" to "value"),
            )

        val jsonString = json.encodeToString(request)
        val decoded = json.decodeFromString<WebSocketRestApiRequest>(jsonString)

        assertEquals(request, decoded)
    }

    @Test
    fun `equality works`() {
        val request1 = WebSocketRestApiRequest("id", "GET", "/path", "body")
        val request2 = WebSocketRestApiRequest("id", "GET", "/path", "body")
        assertEquals(request1, request2)
    }

    @Test
    fun `copy works`() {
        val original = WebSocketRestApiRequest("id", "GET", "/path", "body")
        val copied = original.copy(method = "POST")
        assertEquals("POST", copied.method)
        assertEquals("id", copied.requestId)
        assertEquals("/path", copied.path)
    }

    @Test
    fun `implements WebSocketRequest interface`() {
        val request: WebSocketRequest = WebSocketRestApiRequest("id", "GET", "/path", "body")
        assertEquals("id", request.requestId)
    }

    @Test
    fun `different HTTP methods are supported`() {
        val methods = listOf("GET", "POST", "PUT", "DELETE", "PATCH")
        methods.forEach { method ->
            val request = WebSocketRestApiRequest("id", method, "/path", "body")
            assertEquals(method, request.method)
        }
    }
}
