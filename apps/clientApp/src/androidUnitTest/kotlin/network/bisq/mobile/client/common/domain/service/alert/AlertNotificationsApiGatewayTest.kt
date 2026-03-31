package network.bisq.mobile.client.common.domain.service.alert

import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketRestApiRequest
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketRestApiResponse
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventObserver
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AlertNotificationsApiGatewayTest {
    private val webSocketClientService: WebSocketClientService = mockk()
    private val webSocketApiClient = WebSocketApiClient(webSocketClientService, Json)
    private val gateway = AlertNotificationsApiGateway(webSocketApiClient, webSocketClientService)

    @Test
    fun `subscribeAlerts delegates to websocket client service with alert topic`() =
        runTest {
            val observer = WebSocketEventObserver()
            coEvery {
                webSocketClientService.subscribe(Topic.ALERT_NOTIFICATIONS, "MOBILE_CLIENT")
            } returns observer

            val result = gateway.subscribeAlerts()

            assertSame(observer, result.getOrThrow())
            coVerify(exactly = 1) {
                webSocketClientService.subscribe(Topic.ALERT_NOTIFICATIONS, "MOBILE_CLIENT")
            }
        }

    @Test
    fun `dismissAlert delegates delete request to alert endpoint`() =
        runTest {
            val requestSlot = slot<WebSocketRestApiRequest>()
            coEvery {
                webSocketClientService.sendRequestAndAwaitResponse(capture(requestSlot))
            } returns WebSocketRestApiResponse(requestId = "request-1", statusCode = HttpStatusCode.NoContent.value, body = "")

            val result = gateway.dismissAlert("alert-1")

            assertTrue(result.isSuccess)
            assertEquals("DELETE", requestSlot.captured.method)
            assertEquals("/api/v1/alert-notifications/alert-1", requestSlot.captured.path)
            coVerify(exactly = 1) { webSocketClientService.sendRequestAndAwaitResponse(any()) }
        }
}
