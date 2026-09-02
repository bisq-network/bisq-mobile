package network.bisq.mobile.client.common.domain.websocket.subscription

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.loggerConfigInit
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.data.model.alert.AuthorizedAlertDataDto
import network.bisq.mobile.client.common.domain.service.network.NetworkInfoDto
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketEvent
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationDto
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Decode-level contract of [WebSocketEventPayload.from]: what decodes, what is reported as
 * undecodable (null), and what the failure log may contain. The collector-level skip-and-continue
 * behaviour is covered by [CollectPayloadsTest].
 */
class WebSocketEventPayloadTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val capturedLogs = mutableListOf<String>()
    private val capturedSeverities = mutableListOf<Severity>()
    private lateinit var originalLogger: Logger

    @BeforeTest
    fun captureLogs() {
        capturedLogs.clear()
        capturedSeverities.clear()
        val testWriter =
            object : LogWriter() {
                override fun log(
                    severity: Severity,
                    message: String,
                    tag: String,
                    throwable: Throwable?,
                ) {
                    capturedSeverities += severity
                    capturedLogs += message
                    // kotlinx quotes the JSON input in its exception messages, so a logged
                    // throwable is as much of a leak as a logged payload.
                    throwable?.message?.let { capturedLogs += it }
                    throwable?.let { capturedLogs += it.toString() }
                }
            }
        originalLogger = WebSocketEventPayload.log
        WebSocketEventPayload.log = Logger(loggerConfigInit(testWriter), tag = "WebSocketEventPayload")
    }

    @AfterTest
    fun restoreLogs() {
        WebSocketEventPayload.log = originalLogger
    }

    // Valid payloads

    @Test
    fun `valid payload is decoded`() {
        val decoded = WebSocketEventPayload.from<Int>(json, event(Topic.NUM_USER_PROFILES, "42"))

        assertEquals(42, decoded?.payload)
    }

    @Test
    fun `json null trade restricting alert is a successful null payload`() {
        val decoded =
            WebSocketEventPayload.from<AuthorizedAlertDataDto?>(
                json,
                event(Topic.TRADE_RESTRICTING_ALERT, "null"),
            )

        assertNotNull(decoded, "JSON null is a value, not a decode failure")
        assertNull(decoded.payload)
    }

    @Test
    fun `valid empty list payload is decoded`() {
        val decoded =
            WebSocketEventPayload.from<List<OfferItemPresentationDto>>(
                json,
                event(Topic.OFFERS, "[]"),
            )

        assertEquals(emptyList(), decoded?.payload)
    }

    @Test
    fun `valid empty map payload is decoded`() {
        val decoded =
            WebSocketEventPayload.from<Map<String, Int>>(
                json,
                event(Topic.NUM_OFFERS, "{}"),
            )

        assertEquals(emptyMap(), decoded?.payload)
    }

    // Undecodable payloads

    @Test
    fun `event without payload returns null`() {
        assertNull(WebSocketEventPayload.from<Int>(json, event(Topic.NUM_USER_PROFILES, deferredPayload = null)))
    }

    @Test
    fun `unparseable payloads return null without throwing`() {
        for (payload in listOf("not-json", "", "{")) {
            assertNull(
                WebSocketEventPayload.from<Int>(json, event(Topic.NUM_USER_PROFILES, payload)),
                payload,
            )
        }
    }

    @Test
    fun `version incompatible json returns null without throwing`() {
        for (payload in listOf("{}", "[]")) {
            assertNull(
                WebSocketEventPayload.from<Int>(json, event(Topic.NUM_USER_PROFILES, payload)),
                payload,
            )
        }
    }

    @Test
    fun `dto missing required fields returns null`() {
        assertNull(
            WebSocketEventPayload.from<NetworkInfoDto>(json, event(Topic.NETWORK_INFO, "{}")),
        )
    }

    @Test
    fun `collection with one undecodable element is skipped as a whole`() {
        // No partial delivery: the caller sees either the complete collection or nothing.
        assertNull(
            WebSocketEventPayload.from<Map<String, Int>>(
                json,
                event(Topic.NUM_OFFERS, """{"BTC/USD":3,"BTC/EUR":"three"}"""),
            ),
            "map with one bad value",
        )
        assertNull(
            WebSocketEventPayload.from<List<OfferItemPresentationDto>>(
                json,
                event(Topic.OFFERS, "[{}]"),
            ),
            "list with one bad element",
        )
    }

    @Test
    fun `incompatible payload is skipped and the next valid event is decoded`() {
        val received = mutableListOf<Int>()
        for (event in listOf(event(Topic.NUM_USER_PROFILES, "{}", 1), event(Topic.NUM_USER_PROFILES, "42", 2))) {
            val decoded = WebSocketEventPayload.from<Int>(json, event) ?: continue
            received += decoded.payload
        }

        assertEquals(listOf(42), received)
    }

    // Failure logging

    @Test
    fun `failure is logged as an error naming the topic`() {
        WebSocketEventPayload.from<Int>(json, event(Topic.NUM_USER_PROFILES, "not-json"))

        // Positive control for the leak tests below: if nothing is captured, they pass vacuously.
        assertTrue(capturedSeverities.isNotEmpty(), "Decode failure was not logged")
        assertTrue(capturedSeverities.all { it == Severity.Error }, "Expected error severity, got $capturedSeverities")
        assertTrue(
            capturedLogs.any { it.contains(Topic.NUM_USER_PROFILES.name) },
            "Failure log should name the topic: $capturedLogs",
        )
    }

    @Test
    fun `failure logs do not include payload contents`() {
        val canary = "SECRET_PAYLOAD_CANARY_7f3a"

        val decoded =
            WebSocketEventPayload.from<Int>(
                json,
                event(Topic.NUM_USER_PROFILES, """{"secret":"$canary"}"""),
            )

        assertNull(decoded)
        assertTrue(capturedLogs.isNotEmpty(), "Decode failure was not logged")
        assertFalse(
            capturedLogs.any { it.contains(canary) },
            "Logs leaked payload contents: $capturedLogs",
        )
    }

    @Test
    fun `failure logs do not include the subscriber id`() {
        val subscriberId = "session-CANARY-9c1e"

        WebSocketEventPayload.from<Int>(
            json,
            event(Topic.NUM_USER_PROFILES, "not-json", subscriberId = subscriberId),
        )

        assertTrue(capturedLogs.isNotEmpty(), "Decode failure was not logged")
        assertFalse(
            capturedLogs.any { it.contains(subscriberId) },
            "Logs leaked the subscriber id: $capturedLogs",
        )
    }

    @Test
    fun `successful decode logs nothing`() {
        WebSocketEventPayload.from<Int>(json, event(Topic.NUM_USER_PROFILES, "42"))

        assertTrue(capturedLogs.isEmpty(), "Unexpected logs on success: $capturedLogs")
    }

    private fun event(
        topic: Topic,
        deferredPayload: String?,
        sequenceNumber: Int = 1,
        subscriberId: String = "test-subscriber",
    ) = WebSocketEvent(
        topic = topic,
        subscriberId = subscriberId,
        deferredPayload = deferredPayload,
        modificationType = ModificationType.REPLACE,
        sequenceNumber = sequenceNumber,
    )
}
