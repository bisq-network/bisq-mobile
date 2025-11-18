package network.bisq.mobile.presentation.testutils

import io.ktor.http.URLProtocol
import io.ktor.http.Url
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import network.bisq.mobile.client.websocket.ConnectionState
import network.bisq.mobile.client.websocket.WebSocketClientService

object TestDoubles {
    fun wsService(
        connectDelayMs: Long = 0L,
        testConnectionDelayMs: Long = 0L,
        connectError: Throwable? = null,
        testConnectionError: Throwable? = null,
    ): WebSocketClientService {
        val service = mockk<WebSocketClientService>(relaxed = true)
        every { service.connectionState } returns MutableStateFlow(ConnectionState.Disconnected())
        every { service.determineTimeout(any()) } returns 60_000L
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
                password = any(),
            )
        } coAnswers {
            if (testConnectionDelayMs > 0) delay(testConnectionDelayMs)
            testConnectionError
        }
        return service
    }
}

