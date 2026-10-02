package network.bisq.mobile.client.common.domain.service.trades

import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketRestApiRequest
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketRestApiResponse
import org.junit.Test
import kotlin.test.assertEquals

class TradesApiGatewayTest {
    private val webSocketClientService: WebSocketClientService = mockk()
    private val gateway = TradesApiGateway(WebSocketApiClient(webSocketClientService, Json), webSocketClientService)

    @Test
    fun `isAccountDataBanned gets the check for the trade and decodes the answer`() =
        runTest {
            val requestSlot = slot<WebSocketRestApiRequest>()
            coEvery {
                webSocketClientService.sendRequestAndAwaitResponse(capture(requestSlot))
            } returns
                WebSocketRestApiResponse(
                    requestId = "request-1",
                    statusCode = HttpStatusCode.OK.value,
                    body = """{"banned":true}""",
                )

            val result = gateway.isAccountDataBanned("trade-1")

            assertEquals(AccountDataBannedResponse(banned = true), result.getOrThrow())
            assertEquals("GET", requestSlot.captured.method)
            assertEquals("/api/v1/trades/trade-1/account-data-banned", requestSlot.captured.path)
        }
}
