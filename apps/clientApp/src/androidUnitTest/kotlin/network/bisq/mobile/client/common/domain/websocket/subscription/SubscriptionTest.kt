package network.bisq.mobile.client.common.domain.websocket.subscription

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.service.trades.TradePropertiesDto
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketEvent
import org.junit.Test
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers [Subscription]: a valid event is decoded and delivered to the result handler with its
 * [ModificationType], and subscriptions sharing a single-thread dispatcher (the trade lane).
 *
 * Lives in androidUnitTest (not commonTest) because it needs mockk — [WebSocketClientService] is
 * a concrete class. No Koin/Main setup: [Subscription] runs on [Dispatchers.Default] by default, so
 * delivery is awaited in real time via [runBlocking] + [withTimeout].
 */
class SubscriptionTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val webSocketClientService = mockk<WebSocketClientService>()
    private val lane = Dispatchers.Default.limitedParallelism(1)

    @Test
    fun `subscribe delivers decoded payload and modification type to the result handler`() =
        runBlocking {
            val observer = WebSocketEventObserver()
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

    /** The trade lane is shared, so another subscription's busy handler must not cost this one its snapshot. */
    @Test
    fun `a subscription whose lane is busy handles its snapshot before a later delta`() =
        runBlocking {
            val busyObserver = WebSocketEventObserver()
            val events = WebSocketEventObserver()
            val response = CompletableDeferred<Unit>()
            coEvery { webSocketClientService.subscribe(Topic.TRADE_PROPERTIES, "busy") } returns busyObserver
            coEvery { webSocketClientService.subscribe(Topic.TRADE_PROPERTIES, "under-test") } coAnswers {
                // Like WebSocketClientImpl.subscribe: the observer gets the snapshot once the response arrives.
                response.await()
                events.setEvent(event("snapshot", ModificationType.REPLACE, sequenceNumber = 0))
                events
            }
            val applying = CountDownLatch(1)
            val release = CountDownLatch(1)
            val busy =
                laneSubscription("busy") { _ ->
                    applying.countDown()
                    release.await(10, TimeUnit.SECONDS)
                }
            val handled = Collections.synchronizedList(mutableListOf<String>())
            val subscription = laneSubscription("under-test") { handled += it }
            try {
                busy.subscribe()
                subscription.subscribe()
                busyObserver.setEvent(event("busy", sequenceNumber = 0))
                assertTrue(applying.await(5, TimeUnit.SECONDS))

                response.complete(Unit)
                withTimeout(5_000) { events.hasReceivedData.first { it } }
                // The collector is still waiting for the lane: a delta now must not cost it the snapshot.
                events.setEvent(event("delta", sequenceNumber = 1))
                Thread.sleep(100)
                assertEquals(emptyList(), handled.toList(), "Handled off the lane")

                release.countDown()
                awaitUntil { "ADDED:delta" in handled }
                assertEquals(listOf("REPLACE:snapshot", "ADDED:delta"), handled.toList())
            } finally {
                release.countDown()
                busy.dispose()
                subscription.dispose()
            }
        }

    /** TRADES and TRADE_PROPERTIES share the trade lane, so idle collectors handle their events in arrival order. */
    @Test
    fun `events of subscriptions sharing a lane are handled in arrival order`() =
        runBlocking {
            val blockerObserver = WebSocketEventObserver()
            val firstObserver = WebSocketEventObserver()
            val secondObserver = WebSocketEventObserver()
            coEvery { webSocketClientService.subscribe(Topic.TRADE_PROPERTIES, "blocker") } returns blockerObserver
            coEvery { webSocketClientService.subscribe(Topic.TRADE_PROPERTIES, "first") } returns firstObserver
            coEvery { webSocketClientService.subscribe(Topic.TRADE_PROPERTIES, "second") } returns secondObserver
            var applying = CountDownLatch(1)
            var release = CountDownLatch(1)
            val handled = Collections.synchronizedList(mutableListOf<String>())
            val subscriptions =
                listOf(
                    laneSubscription("blocker") { _ ->
                        applying.countDown()
                        release.await(10, TimeUnit.SECONDS)
                    },
                    laneSubscription("first") { handled += "first-$it" },
                    laneSubscription("second") { handled += "second-$it" },
                )
            try {
                subscriptions.forEach { it.subscribe() }
                firstObserver.setEvent(event("ready", sequenceNumber = 0))
                secondObserver.setEvent(event("ready", sequenceNumber = 0))
                awaitUntil { handled.size == 2 }

                repeat(ROUNDS) { round ->
                    applying = CountDownLatch(1)
                    release = CountDownLatch(1)
                    // With the lane held, both collectors are idle when their events arrive.
                    blockerObserver.setEvent(event("block", sequenceNumber = round))
                    assertTrue(applying.await(5, TimeUnit.SECONDS))
                    // Emitted from Dispatchers.Default, where the WebSocket reader runs.
                    withContext(Dispatchers.Default) {
                        firstObserver.setEvent(event("$round", sequenceNumber = round + 1))
                        secondObserver.setEvent(event("$round", sequenceNumber = round + 1))
                    }
                    release.countDown()
                    awaitUntil { handled.size == 2 + 2 * (round + 1) }
                }

                val expected = (0 until ROUNDS).flatMap { listOf("first-ADDED:$it", "second-ADDED:$it") }
                assertEquals(expected, handled.drop(2))
            } finally {
                release.countDown()
                subscriptions.forEach { it.dispose() }
            }
        }

    /** Subscription on [lane]; [onEvent] gets "TYPE:key" per map key of each payload. */
    private fun laneSubscription(
        parameter: String,
        onEvent: (String) -> Unit,
    ) = Subscription<Map<String, TradePropertiesDto>>(
        webSocketClientService,
        json,
        Topic.TRADE_PROPERTIES,
        resultHandler = { payload, modificationType ->
            payload.flatMap { it.keys }.forEach { onEvent("$modificationType:$it") }
        },
        parameter = parameter,
        dispatcher = lane,
    )

    private fun event(
        key: String,
        modificationType: ModificationType = ModificationType.ADDED,
        sequenceNumber: Int,
    ) = WebSocketEvent(
        topic = Topic.TRADE_PROPERTIES,
        subscriberId = "subscription-test",
        deferredPayload = """[{"$key":{}}]""",
        modificationType = modificationType,
        sequenceNumber = sequenceNumber,
    )

    private suspend fun awaitUntil(condition: () -> Boolean) =
        withTimeout(5_000) {
            while (!condition()) delay(5)
        }

    private companion object {
        const val ROUNDS = 20
    }
}
