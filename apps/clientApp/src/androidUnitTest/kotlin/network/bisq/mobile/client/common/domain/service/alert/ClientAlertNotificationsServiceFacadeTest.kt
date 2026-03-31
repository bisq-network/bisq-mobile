package network.bisq.mobile.client.common.domain.service.alert

import io.mockk.coEvery
import io.mockk.coVerify
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

@OptIn(ExperimentalCoroutinesApi::class)
class ClientAlertNotificationsServiceFacadeTest : KoinIntegrationTestBase() {
    private val apiGateway: AlertNotificationsApiGateway = mockk(relaxed = true)
    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var facade: ClientAlertNotificationsServiceFacade

    override fun onSetup() {
        facade = ClientAlertNotificationsServiceFacade(apiGateway, json)
    }

    @Test
    fun `activate maps supported security alerts and ignores unsupported or blank message alerts`() =
        runTest {
            val observer = WebSocketEventObserver()
            coEvery { apiGateway.subscribeAlerts() } returns Result.success(observer)

            facade.activate()
            observer.setEvent(
                WebSocketEvent(
                    topic = Topic.ALERT_NOTIFICATIONS,
                    subscriberId = "authorized-alerts-test",
                    deferredPayload =
                        """
                        [
                          {
                            "id": "warn-1",
                            "date": 10,
                            "alertType": "WARN",
                            "headline": "Node warning",
                            "message": "  Please update soon.  ",
                            "haltTrading": false,
                            "requireVersionForTrading": true,
                            "minVersion": "2.1.0",
                            "securityManagerProfileId": "sm-1",
                            "appType": "DESKTOP"
                          },
                          {
                            "id": "ban-1",
                            "date": 20,
                            "alertType": "BAN",
                            "message": "Ignored unsupported type"
                          },
                          {
                            "id": "blank-1",
                            "date": 30,
                            "alertType": "INFO",
                            "message": "   "
                          }
                        ]
                        """.trimIndent(),
                    modificationType = ModificationType.REPLACE,
                    sequenceNumber = 1,
                ),
            )

            advanceUntilIdle()

            assertEquals(1, facade.alerts.value.size)
            val alert: AuthorizedAlertData = facade.alerts.value.single()
            assertEquals("warn-1", alert.id)
            assertEquals(AlertType.WARN, alert.type)
            assertEquals("Node warning", alert.headline)
            assertEquals("Please update soon.", alert.message)
            assertEquals(true, alert.requireVersionForTrading)
            assertEquals("2.1.0", alert.minVersion)
        }

    @Test
    fun `dismissAlert removes local alert after backend success`() =
        runTest {
            val observer = WebSocketEventObserver()
            coEvery { apiGateway.subscribeAlerts() } returns Result.success(observer)
            coEvery { apiGateway.dismissAlert("warn-1") } returns Result.success(Unit)

            facade.activate()
            observer.setEvent(
                WebSocketEvent(
                    topic = Topic.ALERT_NOTIFICATIONS,
                    subscriberId = "authorized-alerts-test",
                    deferredPayload =
                        """
                        [
                          {
                            "id": "warn-1",
                            "date": 10,
                            "alertType": "WARN",
                            "message": "Please update soon.",
                            "haltTrading": false,
                            "requireVersionForTrading": false
                          }
                        ]
                        """.trimIndent(),
                    modificationType = ModificationType.REPLACE,
                    sequenceNumber = 1,
                ),
            )
            advanceUntilIdle()

            facade.dismissAlert("warn-1")
            advanceUntilIdle()

            coVerify(exactly = 1) { apiGateway.dismissAlert("warn-1") }
            assertEquals(emptyList(), facade.alerts.value)
        }
}
