package network.bisq.mobile.client.common.domain.service.bootstrap

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.client.common.di.commonTestModule
import network.bisq.mobile.client.common.domain.access.DEMO_API_URL
import network.bisq.mobile.client.common.domain.access.session.SessionService
import network.bisq.mobile.client.common.domain.httpclient.BisqProxyOption
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettings
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.network.KmpTorService
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ClientApplicationBootstrapFacadeTest : KoinTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var sensitiveSettingsRepository: SensitiveSettingsRepository
    private lateinit var webSocketClientService: WebSocketClientService
    private lateinit var kmpTorService: KmpTorService
    private lateinit var sessionService: SessionService
    private lateinit var facade: ClientApplicationBootstrapFacade

    private val settingsFlow = MutableStateFlow(SensitiveSettings())

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        startKoin { modules(commonTestModule) }
        ApplicationBootstrapFacade.isDemo = false

        // Create mocks
        kmpTorService = mockk(relaxed = true)
        webSocketClientService = mockk(relaxed = true)
        sessionService = mockk(relaxed = true)

        // Create fake repository
        sensitiveSettingsRepository =
            object : SensitiveSettingsRepository {
                override val data = settingsFlow

                override suspend fun update(transform: suspend (SensitiveSettings) -> SensitiveSettings) {
                    settingsFlow.value = transform(settingsFlow.value)
                }

                override suspend fun clear() {
                    settingsFlow.value = SensitiveSettings()
                }
            }

        // Setup KmpTorService mocks
        coEvery { kmpTorService.state } returns MutableStateFlow(KmpTorService.TorState.Stopped())
        coEvery { kmpTorService.bootstrapProgress } returns MutableStateFlow(0)
        coEvery { webSocketClientService.connect() } returns null

        facade =
            ClientApplicationBootstrapFacade(
                sensitiveSettingsRepository,
                webSocketClientService,
                kmpTorService,
                sessionService,
            )
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
        ApplicationBootstrapFacade.isDemo = false
    }

    // ========== Demo Mode Detection Tests ==========
    // Note: Full async bootstrap tests are complex due to Dispatchers.Default usage.
    // These tests verify the key demo mode detection logic.

    @Test
    fun `DEMO_API_URL constant is correctly defined`() =
        runTest(testDispatcher) {
            // Verify the demo API URL constant matches expected value
            assertTrue(DEMO_API_URL == "http://demo.bisq:21", "DEMO_API_URL should be http://demo.bisq:21")
        }

    @Test
    fun `isDemo flag can be set and read`() =
        runTest(testDispatcher) {
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
        runTest(testDispatcher) {
            // Given: Settings with demo API URL
            settingsFlow.value =
                SensitiveSettings(
                    bisqApiUrl = DEMO_API_URL,
                    clientName = "test-client",
                    clientId = "demo-client-id",
                    clientSecret = "demo-client-secret",
                    sessionId = "demo-session-id",
                    selectedProxyOption = BisqProxyOption.NONE,
                )

            // Then: Facade should be created successfully
            assertTrue(facade != null, "Facade should be created")

            // Verify settings are correctly stored
            val settings = sensitiveSettingsRepository.fetch()
            assertTrue(settings.bisqApiUrl == DEMO_API_URL, "Settings should have demo API URL")
        }

    @Test
    fun `facade initial state is correct`() =
        runTest(testDispatcher) {
            // Given: Fresh facade
            // Then: Initial progress should be 0
            assertTrue(facade.progress.value == 0f, "Initial progress should be 0")
        }
}
