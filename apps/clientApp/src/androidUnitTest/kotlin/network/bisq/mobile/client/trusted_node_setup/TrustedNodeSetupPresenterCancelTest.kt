package network.bisq.mobile.client.trusted_node_setup

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.client.common.di.clientTestModule
import network.bisq.mobile.client.common.domain.httpclient.BisqProxyOption
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettings
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import network.bisq.mobile.client.common.domain.websocket.ConnectionState
import network.bisq.mobile.client.common.domain.websocket.WebSocketClient
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.test_utils.TestDoubles
import network.bisq.mobile.domain.data.model.User
import network.bisq.mobile.domain.data.repository.UserRepository
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.network.KmpTorService
import network.bisq.mobile.presentation.main.MainPresenter
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class TrustedNodeSetupPresenterCancelTest {
    private lateinit var wsClientService: WebSocketClientService
    private lateinit var kmpTorService: KmpTorService
    private lateinit var appBootstrap: ApplicationBootstrapFacade
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var mainPresenter: MainPresenter

    private lateinit var sensitiveSettingsRepository: SensitiveSettingsRepository
    private lateinit var userRepository: UserRepository

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        // Mocks / fakes
        wsClientService = mockk(relaxed = true)
        kmpTorService = mockk(relaxed = true)
        appBootstrap = mockk(relaxed = true)

        // Minimal repositories
        sensitiveSettingsRepository =
            object : SensitiveSettingsRepository {
                private val _data = MutableStateFlow(SensitiveSettings())
                override val data = _data

                override suspend fun fetch(): SensitiveSettings = _data.value

                override suspend fun update(transform: suspend (t: SensitiveSettings) -> SensitiveSettings) {
                    _data.value = transform(_data.value)
                }

                override suspend fun clear() {
                    _data.value = SensitiveSettings()
                }
            }
        userRepository =
            object : UserRepository {
                private val _data = MutableStateFlow(User())
                override val data = _data

                override suspend fun updateTerms(value: String) {}

                override suspend fun updateStatement(value: String) {}

                override suspend fun update(value: User) {
                    _data.value = value
                }

                override suspend fun clear() {
                    _data.value = User()
                }
            }
        // Mock timeout and connection: delay long so we can cancel
        // IMPORTANT: mock object before stubbing to avoid global leakage across tests
        mockkObject(WebSocketClient)
        every { wsClientService.connectionState } returns MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())
        every { WebSocketClient.determineTimeout(any()) } returns 60_000L
        coEvery { wsClientService.testConnection(any(), any(), any(), any(), any()) } coAnswers {
            delay(10_000)
            null
        }
        // mainPresenter can be a relaxed mock
        mainPresenter = mockk(relaxed = true)

        // Set Main dispatcher for presenterScope
        Dispatchers.setMain(testDispatcher)

        // Start Koin with presentation test module and override WS service
        startKoin {
            modules(
                clientTestModule,
                module {
                    single<WebSocketClientService> { wsClientService }
                },
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDownMain() {
        Dispatchers.resetMain()
    }

    @After
    fun tearDown() {
        stopKoin()
        // Cleanup global mock to avoid leakage across tests
        TestDoubles.cleanupWebSocketClientMock()
    }

    @Test
    fun `cancel during connection resets state and prevents follow-up actions`() =
        runBlocking {
            val presenter =
                TrustedNodeSetupPresenter(
                    mainPresenter,
                    userRepository,
                    sensitiveSettingsRepository,
                    kmpTorService,
                    appBootstrap,
                )
            // Provide a valid API URL and no proxy (so we avoid Tor paths)
            presenter.onApiUrlChanged("http://127.0.0.1:8090")
            presenter.onProxyOptionChanged(BisqProxyOption.NONE)

            // Ensure validation StateFlows are started
            val collectorJob =
                CoroutineScope(testDispatcher).launch {
                    launch { presenter.isApiUrlValid.collect { /* no-op */ } }
                    launch { presenter.isProxyUrlValid.collect { /* no-op */ } }
                }
            // Allow validation flows to propagate
            delay(50)
            // Start connection
            presenter.onTestAndSavePressed(isWorkflow = true)
            // Let things start
            delay(100)
            assertTrue(presenter.isNodeSetupInProgress.value)
            assertTrue(presenter.wsClientConnectionState.value is ConnectionState.Connecting)

            // Cancel
            presenter.onCancelPressed()
            // Give coroutine a beat to process cancellation
            delay(50)
            collectorJob.cancel()

            // Assert state reset
            assertFalse(presenter.isNodeSetupInProgress.value)
            assertEquals("", presenter.status.value)
            val state = presenter.wsClientConnectionState.value
            assertTrue(state is ConnectionState.Disconnected && state.error == null)
            assertEquals(0L, presenter.timeoutCounter.value)

            // Ensure we never proceeded to connect() or disposeClient() after cancel
            coVerify(exactly = 0) { wsClientService.connect() }
            coVerify(exactly = 0) { wsClientService.disposeClient() }
        }
}
