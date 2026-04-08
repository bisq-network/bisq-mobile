package network.bisq.mobile.client.common.domain.service.alert

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketEvent
import network.bisq.mobile.client.common.domain.websocket.subscription.ModificationType
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.client.common.test_utils.KoinIntegrationTestBase
import network.bisq.mobile.domain.model.alert.AlertType
import network.bisq.mobile.domain.model.alert.AuthorizedAlertData
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ClientTradeRestrictingAlertServiceFacadeTest : KoinIntegrationTestBase() {
    private val apiGateway: TradeRestrictingAlertApiGateway = mockk(relaxed = true)
    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var facade: ClientTradeRestrictingAlertServiceFacade

    override fun onSetup() {
        facade = ClientTradeRestrictingAlertServiceFacade(apiGateway, json)
    }

    @Test
    fun `activate maps supported trade restricting alert`() =
        runTest {
            val observer = WebSocketEventObserver()
            coEvery { apiGateway.subscribeAlert() } returns observer

            facade.activate()
            observer.setEvent(
                WebSocketEvent(
                    topic = Topic.TRADE_RESTRICTING_ALERT,
                    subscriberId = "trade-restricting-alert-test",
                    deferredPayload =
                        """
                        {
                          "id": "emergency-1",
                          "date": 10,
                          "alertType": "EMERGENCY",
                          "headline": "Trading halted",
                          "message": "Critical issue detected.",
                          "haltTrading": true,
                          "requireVersionForTrading": false
                        }
                        """.trimIndent(),
                    modificationType = ModificationType.REPLACE,
                    sequenceNumber = 1,
                ),
            )

            advanceUntilIdle()

            val alert: AuthorizedAlertData? = facade.alert.value
            assertEquals("emergency-1", alert?.id)
            assertEquals(AlertType.EMERGENCY, alert?.type)
            assertEquals("Trading halted", alert?.headline)
            assertEquals("Critical issue detected.", alert?.message)
            assertEquals(true, alert?.haltTrading)
            assertEquals(false, alert?.requireVersionForTrading)
            assertNull(alert?.minVersion)
        }

    @Test
    fun `activate clears alert on null payload event`() =
        runTest {
            val observer = WebSocketEventObserver()
            coEvery { apiGateway.subscribeAlert() } returns observer

            facade.activate()
            observer.setEvent(
                WebSocketEvent(
                    topic = Topic.TRADE_RESTRICTING_ALERT,
                    subscriberId = "trade-restricting-alert-test",
                    deferredPayload =
                        """
                        {
                          "id": "emergency-1",
                          "date": 10,
                          "alertType": "EMERGENCY",
                          "message": "Critical issue.",
                          "haltTrading": true
                        }
                        """.trimIndent(),
                    modificationType = ModificationType.REPLACE,
                    sequenceNumber = 1,
                ),
            )
            advanceUntilIdle()
            assertEquals("emergency-1", facade.alert.value?.id)

            observer.setEvent(
                WebSocketEvent(
                    topic = Topic.TRADE_RESTRICTING_ALERT,
                    subscriberId = "trade-restricting-alert-test",
                    deferredPayload = null,
                    modificationType = ModificationType.REPLACE,
                    sequenceNumber = 2,
                ),
            )
            advanceUntilIdle()

            assertNull(facade.alert.value)
        }

    @Test
    fun `activate clears alert when payload is JSON null`() =
        runTest {
            val observer = WebSocketEventObserver()
            coEvery { apiGateway.subscribeAlert() } returns observer

            facade.activate()
            observer.setEvent(
                WebSocketEvent(
                    topic = Topic.TRADE_RESTRICTING_ALERT,
                    subscriberId = "trade-restricting-alert-test",
                    deferredPayload =
                        """
                        {
                          "id": "emergency-1",
                          "date": 10,
                          "alertType": "EMERGENCY",
                          "message": "Critical issue.",
                          "haltTrading": true
                        }
                        """.trimIndent(),
                    modificationType = ModificationType.REPLACE,
                    sequenceNumber = 1,
                ),
            )
            advanceUntilIdle()
            assertEquals("emergency-1", facade.alert.value?.id)

            observer.setEvent(
                WebSocketEvent(
                    topic = Topic.TRADE_RESTRICTING_ALERT,
                    subscriberId = "trade-restricting-alert-test",
                    deferredPayload = "null",
                    modificationType = ModificationType.REPLACE,
                    sequenceNumber = 2,
                ),
            )
            advanceUntilIdle()

            assertNull(facade.alert.value)
        }

    @Test
    fun `activate returns null for unsupported alert type`() =
        runTest {
            val observer = WebSocketEventObserver()
            coEvery { apiGateway.subscribeAlert() } returns observer

            facade.activate()
            observer.setEvent(
                WebSocketEvent(
                    topic = Topic.TRADE_RESTRICTING_ALERT,
                    subscriberId = "trade-restricting-alert-test",
                    deferredPayload =
                        """
                        {
                          "id": "ban-1",
                          "date": 10,
                          "alertType": "BAN",
                          "message": "Banned"
                        }
                        """.trimIndent(),
                    modificationType = ModificationType.REPLACE,
                    sequenceNumber = 1,
                ),
            )
            advanceUntilIdle()

            assertNull(facade.alert.value)
        }

    @Test
    fun `deactivate clears cached alert`() =
        runTest {
            val observer = WebSocketEventObserver()
            coEvery { apiGateway.subscribeAlert() } returns observer

            facade.activate()
            observer.setEvent(validAlertEvent())
            advanceUntilIdle()

            facade.deactivate()

            assertNull(facade.alert.value)
        }

    @Test
    fun `activate ignores malformed payload and keeps previous alert`() =
        runTest {
            val observer = WebSocketEventObserver()
            coEvery { apiGateway.subscribeAlert() } returns observer

            facade.activate()
            observer.setEvent(validAlertEvent(sequenceNumber = 1))
            advanceUntilIdle()

            observer.setEvent(
                WebSocketEvent(
                    topic = Topic.TRADE_RESTRICTING_ALERT,
                    subscriberId = "trade-restricting-alert-test",
                    deferredPayload = "not-json",
                    modificationType = ModificationType.REPLACE,
                    sequenceNumber = 2,
                ),
            )
            advanceUntilIdle()

            assertEquals("emergency-1", facade.alert.value?.id)
        }

    @Test
    fun `activate handles subscription failure without crashing`() =
        runTest {
            coEvery { apiGateway.subscribeAlert() } throws IllegalStateException("boom")

            facade.activate()
            advanceUntilIdle()

            assertNull(facade.alert.value)
        }

    private fun validAlertEvent(sequenceNumber: Int = 1): WebSocketEvent =
        WebSocketEvent(
            topic = Topic.TRADE_RESTRICTING_ALERT,
            subscriberId = "trade-restricting-alert-test",
            deferredPayload =
                """
                {
                  "id": "emergency-1",
                  "date": 10,
                  "alertType": "EMERGENCY",
                  "message": "Critical issue.",
                  "haltTrading": true
                }
                """.trimIndent(),
            modificationType = ModificationType.REPLACE,
            sequenceNumber = sequenceNumber,
        )
}
