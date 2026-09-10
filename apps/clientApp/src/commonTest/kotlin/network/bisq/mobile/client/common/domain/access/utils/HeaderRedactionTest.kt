package network.bisq.mobile.client.common.domain.access.utils

import network.bisq.mobile.client.common.domain.websocket.messages.SubscriptionRequest
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketRestApiRequest
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HeaderRedactionTest {
    private val sessionSecret = "8abad33f-4d69-421f-8250-e53abd81d04e"
    private val clientSecret = "8789beb9-3d54-4e69-90d4-355f9e3bc741"

    @Test
    fun `redactSensitiveHeaders replaces session and client ids`() {
        val headers =
            mapOf(
                Headers.SESSION_ID to sessionSecret,
                Headers.CLIENT_ID to clientSecret,
                "Content-Type" to "application/json",
            )

        val redacted = HeaderRedaction.redactSensitiveHeaders(headers)

        assertEquals(HeaderRedaction.REDACTED, redacted[Headers.SESSION_ID])
        assertEquals(HeaderRedaction.REDACTED, redacted[Headers.CLIENT_ID])
        assertEquals("application/json", redacted["Content-Type"])
    }

    /**
     * The mobile-device registration body carries the FCM/APNs device token and the
     * push-notification symmetric key — the credential that decrypts every relayed push.
     * The WS request log must hold them to the same standard as the auth headers.
     */
    @Test
    fun `redactRawJsonForLogging redacts the device token and symmetric key inside the body`() {
        val deviceToken = "cLnCjmwCTp6UBis7vTTwga:APA91bFVqqlapuBmY"
        val symmetricKey = "FpYYhmDr9/9seiRnIpCyZsABqyXTfnDrLnErdEo4U2s="
        val raw =
            """{"type":"WebSocketRestApiRequest","requestId":"r1","method":"POST",""" +
                """"path":"/api/v1/mobile-devices/registrations",""" +
                """"body":"{\"deviceId\": \"fbac8e\", \"deviceToken\": \"$deviceToken\", """ +
                """\"symmetricKeyBase64\": \"$symmetricKey\", \"platform\": \"ANDROID\"}",""" +
                """"headers":{"Bisq-Session-Id":"$sessionSecret"}}"""

        val redacted = HeaderRedaction.redactRawJsonForLogging(raw)

        assertFalse(redacted.contains(deviceToken), redacted)
        assertFalse(redacted.contains(symmetricKey), redacted)
        assertFalse(redacted.contains(sessionSecret), redacted)
        // Non-sensitive fields survive so the log stays useful.
        assertTrue(redacted.contains("fbac8e"), redacted)
        assertTrue(redacted.contains("ANDROID"), redacted)
    }

    @Test
    fun `redactForLogging redacts sensitive body fields on requests`() {
        val request =
            WebSocketRestApiRequest(
                requestId = "r1",
                method = "POST",
                path = "/api/v1/mobile-devices/registrations",
                body = """{"deviceToken": "secret-token", "symmetricKeyBase64": "secret-key", "deviceId": "fbac8e"}""",
                headers = mapOf(Headers.SESSION_ID to sessionSecret),
            )

        val redacted = HeaderRedaction.redactForLogging(request)

        assertFalse(redacted.contains("secret-token"), redacted)
        assertFalse(redacted.contains("secret-key"), redacted)
        assertFalse(redacted.contains(sessionSecret), redacted)
        assertTrue(redacted.contains("fbac8e"), redacted)
    }

    @Test
    fun `a body without sensitive fields passes through unchanged`() {
        val body = """{"offerId": "o1", "amount": 42}"""

        assertEquals(body, HeaderRedaction.redactSensitiveBodyFields(body))
    }

    @Test
    fun `a sensitive-looking but unparseable body fails closed`() {
        val body = """deviceToken=not-json-at-all"""

        assertEquals(HeaderRedaction.UNPARSEABLE_PAYLOAD, HeaderRedaction.redactSensitiveBodyFields(body))
    }

    @Test
    fun `redactSensitiveHeaders redacts lowercase and mixed-case session and client ids`() {
        val headers =
            mapOf(
                "bisq-session-id" to sessionSecret,
                "Bisq-Client-ID" to clientSecret,
                "Content-Type" to "application/json",
            )

        val redacted = HeaderRedaction.redactSensitiveHeaders(headers)

        assertEquals(HeaderRedaction.REDACTED, redacted["bisq-session-id"])
        assertEquals(HeaderRedaction.REDACTED, redacted["Bisq-Client-ID"])
        assertEquals("application/json", redacted["Content-Type"])
    }

    @Test
    fun `redactForLogging redacts WebSocketRestApiRequest headers`() {
        val request =
            WebSocketRestApiRequest(
                requestId = "req-123",
                method = "GET",
                path = "/api/v1/settings",
                body = "",
                headers =
                    mapOf(
                        Headers.SESSION_ID to sessionSecret,
                        Headers.CLIENT_ID to clientSecret,
                        "X-Custom" to "keep-me",
                    ),
            )

        val logged = HeaderRedaction.redactForLogging(request)

        assertFalse(logged.contains(sessionSecret))
        assertFalse(logged.contains(clientSecret))
        assertTrue(logged.contains(HeaderRedaction.REDACTED))
        assertTrue(logged.contains("X-Custom=keep-me"))
        assertTrue(logged.contains("/api/v1/settings"))
        // Original message must remain untouched for the wire.
        assertEquals(sessionSecret, request.headers[Headers.SESSION_ID])
        assertEquals(clientSecret, request.headers[Headers.CLIENT_ID])
    }

    @Test
    fun `redactForLogging leaves non-rest messages unchanged`() {
        val message = SubscriptionRequest(Topic.REPUTATION, null, "sub-1")

        assertEquals(message.toString(), HeaderRedaction.redactForLogging(message))
    }

    @Test
    fun `redactRawJsonForLogging redacts sensitive header values`() {
        val raw =
            """
            {
              "type": "WebSocketRestApiRequest",
              "requestId": "req-123",
              "method": "GET",
              "path": "/api/v1/settings",
              "body": "",
              "headers": {
                "Bisq-Session-Id": "$sessionSecret",
                "Bisq-Client-Id": "$clientSecret",
                "X-Custom": "keep-me"
              }
            }
            """.trimIndent()

        val logged = HeaderRedaction.redactRawJsonForLogging(raw)

        assertFalse(logged.contains(sessionSecret))
        assertFalse(logged.contains(clientSecret))
        assertTrue(logged.contains(HeaderRedaction.REDACTED))
        assertTrue(logged.contains("keep-me"))
        assertTrue(logged.contains("/api/v1/settings"))
        // Input string is not mutated by reference; verify source still has secrets.
        assertTrue(raw.contains(sessionSecret))
        assertTrue(raw.contains(clientSecret))
    }

    @Test
    fun `redactRawJsonForLogging returns original when no sensitive headers`() {
        val raw = """{"requestId":"1","headers":{"X-Custom":"value"}}"""

        assertEquals(raw, HeaderRedaction.redactRawJsonForLogging(raw))
    }

    @Test
    fun `redactForLogging redacts lowercase and mixed-case session and client ids`() {
        val request =
            WebSocketRestApiRequest(
                requestId = "req-123",
                method = "GET",
                path = "/api/v1/settings",
                body = "",
                headers =
                    mapOf(
                        "bisq-session-id" to sessionSecret,
                        "Bisq-Client-ID" to clientSecret,
                    ),
            )

        val logged = HeaderRedaction.redactForLogging(request)

        assertFalse(logged.contains(sessionSecret))
        assertFalse(logged.contains(clientSecret))
        assertTrue(logged.contains(HeaderRedaction.REDACTED))
    }

    @Test
    fun `redactRawJsonForLogging redacts lowercase and mixed-case session and client ids`() {
        val raw =
            """
            {
              "type": "WebSocketRestApiRequest",
              "requestId": "req-123",
              "method": "GET",
              "path": "/api/v1/settings",
              "body": "",
              "headers": {
                "bisq-session-id": "$sessionSecret",
                "Bisq-Client-ID": "$clientSecret",
                "X-Custom": "keep-me"
              }
            }
            """.trimIndent()

        val logged = HeaderRedaction.redactRawJsonForLogging(raw)

        assertFalse(logged.contains(sessionSecret))
        assertFalse(logged.contains(clientSecret))
        assertTrue(logged.contains(HeaderRedaction.REDACTED))
        assertTrue(logged.contains("keep-me"))
    }

    @Test
    fun `redactRawJsonForLogging fails closed on malformed JSON containing secrets`() {
        // Truncated JSON — parse fails, so the original payload must not be logged.
        val raw =
            """{"headers":{"Bisq-Session-Id":"$sessionSecret","Bisq-Client-Id":"$clientSecret""""

        val logged = HeaderRedaction.redactRawJsonForLogging(raw)

        assertEquals(HeaderRedaction.UNPARSEABLE_PAYLOAD, logged)
        assertFalse(logged.contains(sessionSecret))
        assertFalse(logged.contains(clientSecret))
    }
}
