package network.bisq.mobile.client.common.domain.websocket.subscription

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.service.trades.TradePropertiesDto
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketEvent
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Covers [Subscription]: a valid event is decoded and delivered to the result handler with its
 * [ModificationType].
 *
 * Lives in androidUnitTest (not commonTest) because it needs mockk — [WebSocketClientService] is
 * a concrete class. No Koin/Main setup: [Subscription] hardcodes [Dispatchers.Default], so
 * delivery is awaited in real time via [runBlocking] + [withTimeout].
 */
class SubscriptionTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `subscribe delivers decoded payload and modification type to the result handler`() =
        runBlocking {
            val observer = WebSocketEventObserver()
            val webSocketClientService = mockk<WebSocketClientService>()
            coEvery { webSocketClientService.subscribe(Topic.TRADE_PROPERTIES, null) } returns observer

            val received = CompletableDeferred<Pair<List<Map<String, TradePropertiesDto>>, ModificationType>>()
            val subscription =
                Subscription<Map<String, TradePropertiesDto>>(
                    webSocketClientService,
                    json,
                    Topic.TRADE_PROPERTIES,
                    resultHandler = { payload, modificationType ->
                        received.complete(payload to modificationType)
                    },
                )

            subscription.subscribe()
            observer.setEvent(
                WebSocketEvent(
                    topic = Topic.TRADE_PROPERTIES,
                    subscriberId = "subscription-test",
                    deferredPayload = "[]",
                    modificationType = ModificationType.REPLACE,
                    sequenceNumber = 1,
                ),
            )

            val (payload, modificationType) = withTimeout(5_000) { received.await() }
            assertEquals(emptyList(), payload)
            assertEquals(ModificationType.REPLACE, modificationType)

            subscription.dispose()
        }
}
