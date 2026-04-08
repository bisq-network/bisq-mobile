package network.bisq.mobile.client.common.domain.service.alert

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventObserver
import org.junit.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class TradeRestrictingAlertApiGatewayTest {
    private val webSocketClientService: WebSocketClientService = mockk()
    private val gateway = TradeRestrictingAlertApiGateway(webSocketClientService)

    @Test
    fun `subscribeAlert delegates to websocket client service with trade restricting alert topic`() =
        runTest {
            val observer = WebSocketEventObserver()
            coEvery {
                webSocketClientService.subscribe(Topic.TRADE_RESTRICTING_ALERT, "MOBILE_CLIENT")
            } returns observer

            val result = gateway.subscribeAlert()

            assertSame(observer, result)
            coVerify(exactly = 1) {
                webSocketClientService.subscribe(Topic.TRADE_RESTRICTING_ALERT, "MOBILE_CLIENT")
            }
        }

    @Test
    fun `subscribeAlert propagates exception when websocket subscription throws`() =
        runTest {
            coEvery {
                webSocketClientService.subscribe(Topic.TRADE_RESTRICTING_ALERT, "MOBILE_CLIENT")
            } throws IllegalStateException("connection closed")

            assertFailsWith<IllegalStateException> {
                gateway.subscribeAlert()
            }
        }
}
