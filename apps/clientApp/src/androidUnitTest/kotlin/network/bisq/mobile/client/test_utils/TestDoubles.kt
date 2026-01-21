package network.bisq.mobile.client.test_utils

import io.ktor.http.Url
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.client.common.domain.websocket.ConnectionState
import network.bisq.mobile.client.common.domain.websocket.WebSocketClient
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService

object TestDoubles {
    fun wsService(
        connectDelayMs: Long = 0L,
        testConnectionDelayMs: Long = 0L,
        connectError: Throwable? = null,
        testConnectionError: Throwable? = null,
        timeoutMs: Long = 60_000L,
    ): Pair<WebSocketClientService, () -> Unit> {
        val service = mockk<WebSocketClientService>(relaxed = true)
        every { service.connectionState } returns MutableStateFlow(ConnectionState.Disconnected())
        mockkObject(WebSocketClient)
        every { WebSocketClient.determineTimeout(any()) } returns timeoutMs
        coEvery { service.connect() } coAnswers {
            if (connectDelayMs > 0) delay(connectDelayMs)
            connectError
        }
        coEvery {
            service.testConnection(
                apiUrl = any<Url>(),
                proxyHost = any(),
                proxyPort = any(),
                isTorProxy = any(),
            )
        } coAnswers {
            if (testConnectionDelayMs > 0) delay(testConnectionDelayMs)
            testConnectionError
        }
        // Return service and a cleanup function to avoid global MockK leakage
        return service to { cleanupWebSocketClientMock() }
    }

    fun cleanupWebSocketClientMock() {
        unmockkObject(WebSocketClient)
    }
}
