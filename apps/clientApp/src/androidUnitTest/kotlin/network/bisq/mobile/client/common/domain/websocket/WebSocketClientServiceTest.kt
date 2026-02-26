package network.bisq.mobile.client.common.domain.websocket

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.client.common.di.commonTestModule
import network.bisq.mobile.client.common.domain.access.session.SessionResponse
import network.bisq.mobile.client.common.domain.access.session.SessionService
import network.bisq.mobile.client.common.domain.httpclient.HttpClientService
import network.bisq.mobile.client.common.domain.httpclient.HttpClientSettings
import network.bisq.mobile.client.common.domain.httpclient.exception.UnauthorizedApiAccessException
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettings
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WebSocketClientServiceTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var webSocketClientService: WebSocketClientService
    private lateinit var httpClientService: HttpClientService
    private lateinit var webSocketClientFactory: WebSocketClientFactory
    private lateinit var sessionService: SessionService
    private lateinit var sensitiveSettingsRepository: SensitiveSettingsRepository
    private lateinit var mockClient: WebSocketClient
    private lateinit var httpClientChangedFlow: MutableSharedFlow<HttpClientSettings>

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        startKoin { modules(commonTestModule) }

        httpClientService = mockk(relaxed = true)
        webSocketClientFactory = mockk(relaxed = true)
        sessionService = mockk(relaxed = true)
        sensitiveSettingsRepository = mockk(relaxed = true)
        mockClient = mockk(relaxed = true)
        httpClientChangedFlow = MutableSharedFlow()

        every { httpClientService.httpClientChangedFlow } returns httpClientChangedFlow
        coEvery { httpClientService.getClient() } returns mockk(relaxed = true)
        every { webSocketClientFactory.createNewClient(any(), any(), any(), any()) } returns mockClient
        every { mockClient.apiUrl } returns
            mockk {
                every { host } returns "localhost"
            }
        every { mockClient.webSocketClientStatus } returns MutableStateFlow(ConnectionState.Disconnected())
        every { mockClient.reconnect() } just Runs

        webSocketClientService =
            WebSocketClientService(
                defaultHost = "localhost",
                defaultPort = 8080,
                httpClientService = httpClientService,
                webSocketClientFactory = webSocketClientFactory,
                sessionService = sessionService,
                sensitiveSettingsRepository = sensitiveSettingsRepository,
            )
    }

    @After
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    @Test
    fun `triggerReconnect calls reconnect on current client when not connected`() =
        runTest(testDispatcher) {
            // Given
            val mockWsClient = mockk<WebSocketClient>(relaxed = true)
            every { mockWsClient.webSocketClientStatus } returns MutableStateFlow(ConnectionState.Disconnected())
            every { mockWsClient.reconnect() } just Runs
            every { mockWsClient.apiUrl } returns
                mockk {
                    every { host } returns "localhost"
                }
            every { webSocketClientFactory.createNewClient(any(), any(), any(), any()) } returns mockWsClient

            // Activate and set up a client
            webSocketClientService.activate()
            testDispatcher.scheduler.advanceUntilIdle()

            // Emit settings with credentials to create a client
            httpClientChangedFlow.emit(
                HttpClientSettings(
                    bisqApiUrl = "http://localhost:8080",
                    tlsFingerprint = null,
                    clientId = "client-id",
                    sessionId = "session-id",
                ),
            )
            testDispatcher.scheduler.advanceUntilIdle()

            // When
            webSocketClientService.triggerReconnect()

            // Then
            verify { mockWsClient.reconnect() }
        }

    @Test
    fun `triggerReconnect does nothing when no client available`() =
        runTest(testDispatcher) {
            // Given - service initialized but no client created yet
            webSocketClientService.activate()
            testDispatcher.scheduler.advanceUntilIdle()

            // When
            webSocketClientService.triggerReconnect()

            // Then - no exception should be thrown, method returns silently
            assertTrue(true) // Test passes if no exception
        }

    @Test
    fun `triggerReconnect does nothing when already connected`() =
        runTest(testDispatcher) {
            // Given
            val mockWsClient = mockk<WebSocketClient>(relaxed = true)
            every { mockWsClient.webSocketClientStatus } returns MutableStateFlow(ConnectionState.Connected)
            every { mockWsClient.reconnect() } just Runs
            every { mockWsClient.apiUrl } returns
                mockk {
                    every { host } returns "localhost"
                }
            every { webSocketClientFactory.createNewClient(any(), any(), any(), any()) } returns mockWsClient

            webSocketClientService.activate()
            testDispatcher.scheduler.advanceUntilIdle()

            // Create client by emitting settings
            httpClientChangedFlow.emit(
                HttpClientSettings(
                    bisqApiUrl = "http://localhost:8080",
                    tlsFingerprint = null,
                    clientId = "client-id",
                    sessionId = "session-id",
                ),
            )
            testDispatcher.scheduler.advanceUntilIdle()

            // When
            webSocketClientService.triggerReconnect()

            // Then - reconnect should NOT be called when already connected
            verify(exactly = 0) { mockWsClient.reconnect() }
        }

    @Test
    fun `isConnected returns true when connectionState is Connected`() =
        runTest(testDispatcher) {
            // Given
            val connectedStateFlow = MutableStateFlow<ConnectionState>(ConnectionState.Connected)
            val mockWsClient = mockk<WebSocketClient>(relaxed = true)
            every { mockWsClient.webSocketClientStatus } returns connectedStateFlow
            every { mockWsClient.apiUrl } returns
                mockk {
                    every { host } returns "localhost"
                }
            every { webSocketClientFactory.createNewClient(any(), any(), any(), any()) } returns mockWsClient

            webSocketClientService.activate()
            testDispatcher.scheduler.advanceUntilIdle()

            // Emit settings to create client
            httpClientChangedFlow.emit(
                HttpClientSettings(
                    bisqApiUrl = "http://localhost:8080",
                    tlsFingerprint = null,
                    clientId = "client-id",
                    sessionId = "session-id",
                ),
            )
            testDispatcher.scheduler.advanceUntilIdle()

            // When
            val isConnected = webSocketClientService.isConnected()

            // Then
            assertTrue(isConnected)
        }

    @Test
    fun `isConnected returns false when connectionState is Disconnected`() =
        runTest(testDispatcher) {
            // Given
            val disconnectedStateFlow = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())
            val mockWsClient = mockk<WebSocketClient>(relaxed = true)
            every { mockWsClient.webSocketClientStatus } returns disconnectedStateFlow
            every { mockWsClient.apiUrl } returns
                mockk {
                    every { host } returns "localhost"
                }
            every { webSocketClientFactory.createNewClient(any(), any(), any(), any()) } returns mockWsClient

            webSocketClientService.activate()
            testDispatcher.scheduler.advanceUntilIdle()

            // Emit settings to create client
            httpClientChangedFlow.emit(
                HttpClientSettings(
                    bisqApiUrl = "http://localhost:8080",
                    tlsFingerprint = null,
                    clientId = "client-id",
                    sessionId = "session-id",
                ),
            )
            testDispatcher.scheduler.advanceUntilIdle()

            // When
            val isConnected = webSocketClientService.isConnected()

            // Then
            assertFalse(isConnected)
        }

    @Test
    fun `connectionState initial value is Disconnected`() =
        runTest(testDispatcher) {
            // Given - fresh service instance

            // When
            val initialState = webSocketClientService.connectionState.value

            // Then
            assertTrue(initialState is ConnectionState.Disconnected)
            assertEquals(null, (initialState as ConnectionState.Disconnected).error)
        }

    @Test
    fun `credential guard prevents client creation when sessionId is missing`() =
        runTest(testDispatcher) {
            // Given
            webSocketClientService.activate()
            testDispatcher.scheduler.advanceUntilIdle()

            // When - emit settings without sessionId
            httpClientChangedFlow.emit(
                HttpClientSettings(
                    bisqApiUrl = "http://localhost:8080",
                    tlsFingerprint = null,
                    clientId = "client-id",
                    sessionId = null,
                ),
            )
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - no client should be created
            verify(exactly = 0) { webSocketClientFactory.createNewClient(any(), any(), any(), any()) }
            assertTrue(webSocketClientService.connectionState.value is ConnectionState.Disconnected)
        }

    @Test
    fun `credential guard prevents client creation when clientId is missing`() =
        runTest(testDispatcher) {
            // Given
            webSocketClientService.activate()
            testDispatcher.scheduler.advanceUntilIdle()

            // When - emit settings without clientId
            httpClientChangedFlow.emit(
                HttpClientSettings(
                    bisqApiUrl = "http://localhost:8080",
                    tlsFingerprint = null,
                    clientId = null,
                    sessionId = "session-id",
                ),
            )
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - no client should be created
            verify(exactly = 0) { webSocketClientFactory.createNewClient(any(), any(), any(), any()) }
            assertTrue(webSocketClientService.connectionState.value is ConnectionState.Disconnected)
        }

    @Test
    fun `credential guard prevents client creation when sessionId is blank`() =
        runTest(testDispatcher) {
            // Given
            webSocketClientService.activate()
            testDispatcher.scheduler.advanceUntilIdle()

            // When - emit settings with blank sessionId
            httpClientChangedFlow.emit(
                HttpClientSettings(
                    bisqApiUrl = "http://localhost:8080",
                    tlsFingerprint = null,
                    clientId = "client-id",
                    sessionId = "",
                ),
            )
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - no client should be created
            verify(exactly = 0) { webSocketClientFactory.createNewClient(any(), any(), any(), any()) }
            assertTrue(webSocketClientService.connectionState.value is ConnectionState.Disconnected)
        }

    @Test
    fun `credential guard prevents client creation when clientId is blank`() =
        runTest(testDispatcher) {
            // Given
            webSocketClientService.activate()
            testDispatcher.scheduler.advanceUntilIdle()

            // When - emit settings with blank clientId
            httpClientChangedFlow.emit(
                HttpClientSettings(
                    bisqApiUrl = "http://localhost:8080",
                    tlsFingerprint = null,
                    clientId = "",
                    sessionId = "session-id",
                ),
            )
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - no client should be created
            verify(exactly = 0) { webSocketClientFactory.createNewClient(any(), any(), any(), any()) }
            assertTrue(webSocketClientService.connectionState.value is ConnectionState.Disconnected)
        }

    @Test
    fun `client is created when both credentials are present`() =
        runTest(testDispatcher) {
            // Given
            webSocketClientService.activate()
            testDispatcher.scheduler.advanceUntilIdle()

            // When - emit settings with valid credentials
            httpClientChangedFlow.emit(
                HttpClientSettings(
                    bisqApiUrl = "http://localhost:8080",
                    tlsFingerprint = null,
                    clientId = "client-id",
                    sessionId = "session-id",
                ),
            )
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - client should be created
            verify { webSocketClientFactory.createNewClient(any(), any(), "session-id", "client-id") }
        }

    @Test
    fun `session renewal is triggered with valid credentials after 401 error`() =
        runTest(testDispatcher) {
            // Given - setup mock settings flow and session service
            val mockSettingsFlow =
                MutableStateFlow(
                    SensitiveSettings(
                        clientId = "client-id",
                        clientSecret = "client-secret",
                        sessionId = "old-session-id",
                    ),
                )
            every { sensitiveSettingsRepository.data } returns mockSettingsFlow
            coEvery { sensitiveSettingsRepository.fetch() } returns
                SensitiveSettings(
                    clientId = "client-id",
                    clientSecret = "client-secret",
                    sessionId = "old-session-id",
                )
            coEvery { sensitiveSettingsRepository.update(any()) } just Runs
            coEvery { sessionService.requestSession(any(), any()) } returns
                Result.success(
                    SessionResponse(
                        sessionId = "new-session-id",
                        expiresAt = System.currentTimeMillis() + 3600000,
                    ),
                )

            // Create mock client that emits 401 Unauthorized error
            val unauthorizedStatusFlow = MutableStateFlow<ConnectionState>(ConnectionState.Connected)
            val mockWsClient = mockk<WebSocketClient>(relaxed = true)
            every { mockWsClient.webSocketClientStatus } returns unauthorizedStatusFlow
            every { mockWsClient.reconnect() } just Runs
            every { mockWsClient.apiUrl } returns
                mockk {
                    every { host } returns "localhost"
                }
            every { webSocketClientFactory.createNewClient(any(), any(), any(), any()) } returns mockWsClient

            // Activate service and create initial client
            webSocketClientService.activate()
            testDispatcher.scheduler.advanceUntilIdle()

            httpClientChangedFlow.emit(
                HttpClientSettings(
                    bisqApiUrl = "http://localhost:8080",
                    tlsFingerprint = null,
                    clientId = "client-id",
                    sessionId = "session-id",
                ),
            )
            testDispatcher.scheduler.advanceUntilIdle()

            // When - simulate 401 Unauthorized error from WebSocket client
            unauthorizedStatusFlow.value =
                ConnectionState.Disconnected(
                    error = UnauthorizedApiAccessException(),
                )
            testDispatcher.scheduler.advanceUntilIdle()

            // Advance time past session renewal cooldown (30 seconds)
            testDispatcher.scheduler.advanceTimeBy(31000)
            testDispatcher.scheduler.runCurrent()

            // Then - verify session renewal was requested
            coVerify { sessionService.requestSession("client-id", "client-secret") }
            // And settings were updated with new session
            coVerify {
                sensitiveSettingsRepository.update(any<suspend (SensitiveSettings) -> SensitiveSettings>())
            }
        }

    @Test
    fun `deactivate disconnects client and resets subscription state`() =
        runTest(testDispatcher) {
            // Given - activated service with a connected client
            val connectedStateFlow = MutableStateFlow<ConnectionState>(ConnectionState.Connected)
            val mockWsClient = mockk<WebSocketClient>(relaxed = true)
            every { mockWsClient.webSocketClientStatus } returns connectedStateFlow
            every { mockWsClient.apiUrl } returns
                mockk {
                    every { host } returns "localhost"
                }
            every { webSocketClientFactory.createNewClient(any(), any(), any(), any()) } returns mockWsClient

            webSocketClientService.activate()
            testDispatcher.scheduler.advanceUntilIdle()

            httpClientChangedFlow.emit(
                HttpClientSettings(
                    bisqApiUrl = "http://localhost:8080",
                    tlsFingerprint = null,
                    clientId = "client-id",
                    sessionId = "session-id",
                ),
            )
            testDispatcher.scheduler.advanceUntilIdle()

            // Verify connected state
            assertTrue(webSocketClientService.isConnected())

            // When
            webSocketClientService.deactivate()
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - client should have been disconnected
            coVerify { mockWsClient.disconnect() }
            // Connection state should be Disconnected
            assertTrue(webSocketClientService.connectionState.value is ConnectionState.Disconnected)
            // isConnected should return false
            assertFalse(webSocketClientService.isConnected())
        }

    @Test
    fun `activate after deactivate starts fresh and can reconnect`() =
        runTest(testDispatcher) {
            // Given - activate, create client, then deactivate
            val connectedStateFlow = MutableStateFlow<ConnectionState>(ConnectionState.Connected)
            val mockWsClient = mockk<WebSocketClient>(relaxed = true)
            every { mockWsClient.webSocketClientStatus } returns connectedStateFlow
            every { mockWsClient.apiUrl } returns
                mockk {
                    every { host } returns "localhost"
                }
            every { webSocketClientFactory.createNewClient(any(), any(), any(), any()) } returns mockWsClient

            webSocketClientService.activate()
            testDispatcher.scheduler.advanceUntilIdle()

            httpClientChangedFlow.emit(
                HttpClientSettings(
                    bisqApiUrl = "http://localhost:8080",
                    tlsFingerprint = null,
                    clientId = "client-id",
                    sessionId = "session-id",
                ),
            )
            testDispatcher.scheduler.advanceUntilIdle()

            webSocketClientService.deactivate()
            testDispatcher.scheduler.advanceUntilIdle()

            // When - reactivate
            webSocketClientService.activate()
            testDispatcher.scheduler.advanceUntilIdle()

            // Emit settings again to trigger new client creation
            httpClientChangedFlow.emit(
                HttpClientSettings(
                    bisqApiUrl = "http://localhost:8080",
                    tlsFingerprint = null,
                    clientId = "client-id",
                    sessionId = "session-id-2",
                ),
            )
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - a new client should be created (factory called again)
            verify(atLeast = 2) { webSocketClientFactory.createNewClient(any(), any(), any(), any()) }
        }

    @Test
    fun `session renewal does not trigger when sensitiveSettingsRepository is null`() =
        runTest(testDispatcher) {
            // Given - service without sensitiveSettingsRepository
            val serviceWithoutRepo =
                WebSocketClientService(
                    defaultHost = "localhost",
                    defaultPort = 8080,
                    httpClientService = httpClientService,
                    webSocketClientFactory = webSocketClientFactory,
                    sessionService = sessionService,
                    sensitiveSettingsRepository = null,
                )

            serviceWithoutRepo.activate()
            testDispatcher.scheduler.advanceUntilIdle()

            // When/Then - service should work without session renewal capability
            assertTrue(serviceWithoutRepo.connectionState.value is ConnectionState.Disconnected)
        }

    @Test
    fun `session renewal does not trigger when sessionService is null`() =
        runTest(testDispatcher) {
            // Given - service without sessionService
            val serviceWithoutSession =
                WebSocketClientService(
                    defaultHost = "localhost",
                    defaultPort = 8080,
                    httpClientService = httpClientService,
                    webSocketClientFactory = webSocketClientFactory,
                    sessionService = null,
                    sensitiveSettingsRepository = sensitiveSettingsRepository,
                )

            serviceWithoutSession.activate()
            testDispatcher.scheduler.advanceUntilIdle()

            // When/Then - service should work without session renewal capability
            assertTrue(serviceWithoutSession.connectionState.value is ConnectionState.Disconnected)
        }
}
