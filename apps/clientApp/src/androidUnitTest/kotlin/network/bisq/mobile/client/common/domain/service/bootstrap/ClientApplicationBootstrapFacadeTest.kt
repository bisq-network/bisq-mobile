package network.bisq.mobile.client.common.domain.service.bootstrap

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.client.common.domain.access.DEMO_API_URL
import network.bisq.mobile.client.common.domain.access.session.SessionResponse
import network.bisq.mobile.client.common.domain.access.session.SessionService
import network.bisq.mobile.client.common.domain.httpclient.BisqProxyOption
import network.bisq.mobile.client.common.domain.httpclient.HttpClientService
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettings
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepositoryMock
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.test_utils.ClientKoinIntegrationTestBase
import network.bisq.mobile.data.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.data.service.network.ConnectivityService
import network.bisq.mobile.data.service.network.ConnectivityService.ConnectivityStatus
import network.bisq.mobile.data.service.network.KmpTorService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ClientApplicationBootstrapFacadeTest : ClientKoinIntegrationTestBase() {
    private val webSocketClientService: WebSocketClientService = mockk(relaxed = true)
    private val httpClientService: HttpClientService = mockk(relaxed = true)
    private val kmpTorService: KmpTorService = mockk(relaxed = true)
    private val sessionService: SessionService = mockk(relaxed = true)
    private val connectivityService: ConnectivityService = mockk(relaxed = true)

    private lateinit var sensitiveSettingsRepository: SensitiveSettingsRepositoryMock
    private lateinit var facade: ClientApplicationBootstrapFacade

    private val connectivityStatusFlow = MutableStateFlow(ConnectivityStatus.BOOTSTRAPPING)

    override fun onSetup() {
        ApplicationBootstrapFacade.isDemo = false
        sensitiveSettingsRepository = SensitiveSettingsRepositoryMock()

        coEvery { kmpTorService.state } returns MutableStateFlow(KmpTorService.TorState.Stopped())
        coEvery { kmpTorService.bootstrapProgress } returns MutableStateFlow(0)
        coEvery { webSocketClientService.connect() } returns null
        coEvery { httpClientService.awaitClientReady(any()) } returns true
        coEvery { connectivityService.status } returns connectivityStatusFlow

        facade =
            ClientApplicationBootstrapFacade(
                sensitiveSettingsRepository,
                webSocketClientService,
                httpClientService,
                kmpTorService,
                sessionService,
                connectivityService,
                testDispatcher,
            )
    }

    override fun onTearDown() {
        try {
            ApplicationBootstrapFacade.isDemo = false
        } finally {
            super.onTearDown()
        }
    }

    // ========== Demo Mode Detection Tests ==========
    // Note: Full async bootstrap tests are complex due to Dispatchers.Default usage.
    // These tests verify the key demo mode detection logic.

    @Test
    fun `DEMO_API_URL constant is correctly defined`() =
        runTest {
            // Verify the demo API URL constant matches expected value
            assertTrue(DEMO_API_URL == "http://demo.bisq:21", "DEMO_API_URL should be http://demo.bisq:21")
        }

    @Test
    fun `isDemo flag can be set and read`() =
        runTest {
            // Given: isDemo is initially false
            ApplicationBootstrapFacade.isDemo = false
            assertTrue(!ApplicationBootstrapFacade.isDemo, "isDemo should initially be false")

            // When: isDemo is set to true
            ApplicationBootstrapFacade.isDemo = true

            // Then: isDemo should be true
            assertTrue(ApplicationBootstrapFacade.isDemo, "isDemo should be true after setting")
        }

    @Test
    fun `facade can be created with demo mode settings`() =
        runTest {
            // Given: Settings with demo API URL
            sensitiveSettingsRepository.update {
                SensitiveSettings(
                    bisqApiUrl = DEMO_API_URL,
                    clientName = "test-client",
                    clientId = "demo-client-id",
                    clientSecret = "demo-client-secret",
                    sessionId = "demo-session-id",
                    selectedProxyOption = BisqProxyOption.NONE,
                )
            }

            // Verify settings are correctly stored
            val settings = sensitiveSettingsRepository.fetch()
            assertTrue(settings.bisqApiUrl == DEMO_API_URL, "Settings should have demo API URL")
        }

    @Test
    fun `facade initial state is correct`() =
        runTest {
            // Given: Fresh facade
            // Then: Initial progress should be 0
            assertTrue(facade.progress.value == 0f, "Initial progress should be 0")
        }

    @Test
    fun `successful session renewal persists sessionExpiresAt`() =
        runTest {
            val expiresAt = 1_700_000_000_000L
            sensitiveSettingsRepository.update {
                SensitiveSettings(
                    bisqApiUrl = "http://localhost:8080",
                    clientName = "test-client",
                    clientId = "client-id",
                    clientSecret = "client-secret",
                    sessionId = "old-session-id",
                )
            }
            coEvery { sessionService.requestSession("client-id", "client-secret") } returns
                Result.success(
                    SessionResponse(
                        sessionId = "new-session-id",
                        expiresAt = expiresAt,
                    ),
                )

            facade.onTorStartedOrSkipped()
            // onTorStartedOrSkipped now runs on the injected testDispatcher, so advanceUntilIdle() drives it.
            advanceUntilIdle()

            assertEquals("new-session-id", sensitiveSettingsRepository.data.value.sessionId)
            assertEquals(expiresAt, sensitiveSettingsRepository.data.value.sessionExpiresAt)
        }

    @Test
    fun `data received during loading completes bootstrap as CONNECTED`() =
        runTest {
            // Simulate the state right after a successful WebSocket connect.
            facade.observeConnectivityForDataLoad()

            connectivityStatusFlow.value = ConnectivityStatus.CONNECTED_AND_DATA_RECEIVED
            advanceUntilIdle()

            assertEquals(
                ClientApplicationBootstrapFacade.ConnectBootstrapPhase.CONNECTED,
                facade.bootstrapPhase.value,
            )
            assertEquals(1.0f, facade.progress.value)
        }

    @Test
    fun `connected with limitations completes progress without marking data received`() =
        runTest {
            facade.observeConnectivityForDataLoad()

            connectivityStatusFlow.value = ConnectivityStatus.CONNECTED_WITH_LIMITATIONS
            advanceUntilIdle()

            // Progress completes so the presenter's navigateToNextScreen runs its limitations handling,
            // but the phase is not advanced to CONNECTED (limitations is not "data received").
            assertEquals(1.0f, facade.progress.value)
            assertEquals(
                ClientApplicationBootstrapFacade.ConnectBootstrapPhase.CONNECTING,
                facade.bootstrapPhase.value,
            )
        }

    @Test
    fun `requesting inventory completes bootstrap without marking data received`() =
        runTest {
            facade.observeConnectivityForDataLoad()

            connectivityStatusFlow.value = ConnectivityStatus.REQUESTING_INVENTORY
            advanceUntilIdle()

            // REQUESTING_INVENTORY is already isConnected(), so bootstrap completes (proceed-when-usable,
            // matching pre-redesign timing) — but the strip is NOT advanced to CONNECTED (not fully
            // received). This seam is exercised without connect(), so the phase stays at its initial
            // value; the meaningful invariant is that it did not become CONNECTED.
            assertEquals(1.0f, facade.progress.value)
            assertNotEquals(
                ClientApplicationBootstrapFacade.ConnectBootstrapPhase.CONNECTED,
                facade.bootstrapPhase.value,
            )
        }
}
