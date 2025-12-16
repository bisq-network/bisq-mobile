package network.bisq.mobile.client.trusted_node_setup

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import network.bisq.mobile.client.common.di.clientTestModule
import network.bisq.mobile.client.common.domain.httpclient.BisqProxyOption
import network.bisq.mobile.client.common.domain.websocket.ConnectionState
import network.bisq.mobile.client.common.domain.websocket.WebSocketClient
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettings
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import network.bisq.mobile.client.test_utils.TestDoubles
import network.bisq.mobile.domain.data.model.User
import network.bisq.mobile.domain.data.repository.UserRepository
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.network.KmpTorService
import network.bisq.mobile.presentation.main.MainPresenter
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

@ExperimentalCoroutinesApi
class TrustedNodeSetupPresenterTimeoutTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var wsClientService: WebSocketClientService
    private lateinit var kmpTorService: KmpTorService
    private lateinit var appBootstrap: ApplicationBootstrapFacade
    private lateinit var mainPresenter: MainPresenter

    private lateinit var sensitiveSettingsRepository: SensitiveSettingsRepository
    private lateinit var userRepository: UserRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Mocks / fakes
        wsClientService = mockk(relaxed = true)
        kmpTorService = mockk(relaxed = true)
        appBootstrap = mockk(relaxed = true)
        mainPresenter = mockk(relaxed = true)

        // Minimal repositories
        sensitiveSettingsRepository = object : SensitiveSettingsRepository {
            private val _data = MutableStateFlow(SensitiveSettings())
            override val data = _data
            override suspend fun fetch(): SensitiveSettings = _data.value
            override suspend fun update(transform: suspend (t: SensitiveSettings) -> SensitiveSettings) { _data.value = transform(_data.value) }
            override suspend fun clear() { _data.value = SensitiveSettings() }
        }
        userRepository = object : UserRepository {
            private val _data = MutableStateFlow(User())
            override val data = _data
            override suspend fun updateTerms(value: String) {}
            override suspend fun updateStatement(value: String) {}
            override suspend fun update(value: User) { _data.value = value }
            override suspend fun clear() { _data.value = User() }
        }

        // IMPORTANT: mock object before stubbing to avoid global leakage across tests
        mockkObject(WebSocketClient)

        // Mock timeout behavior
        every { wsClientService.connectionState } returns MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())
        every { WebSocketClient.determineTimeout(any()) } returns 3_000L
        coEvery { wsClientService.testConnection(any(), any(), any(), any(), any()) } coAnswers {
            // Simulate a timeout that returns the TimeoutCancellationException as a result
            val e = runCatching { withTimeout(1) { delay(50) } }.exceptionOrNull()
            e
        }

        startKoin {
            modules(
                clientTestModule,
                module {
                    single<WebSocketClientService> { wsClientService }
                    single<KmpTorService> { kmpTorService }
                    single<ApplicationBootstrapFacade> { appBootstrap }
                },
            )
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
        // Cleanup global mock to avoid leakage across tests
        TestDoubles.cleanupWebSocketClientMock()
    }

    @Ignore("Flaky under current test jobs manager; will enable after injecting test jobs manager here")
    @Test
    fun `timeout during connection is routed to error handler (not silent cancel)`() = runTest {
        val presenter = TrustedNodeSetupPresenter(
            mainPresenter,
            userRepository,
            sensitiveSettingsRepository,
            kmpTorService,
            appBootstrap,
        )

        // Set valid inputs
        presenter.onApiUrlChanged("http://127.0.0.1:8090")
        presenter.onProxyOptionChanged(BisqProxyOption.NONE)

        // Start validators (lazy flows)
        val validators = backgroundScope.launch {
            launch { presenter.isApiUrlValid.collect { } }
            launch { presenter.isProxyUrlValid.collect { } }
        }

        // Act
        presenter.onTestAndSavePressed(isWorkflow = true)

        // Run all scheduled tasks/timeouts
        advanceUntilIdle()
        validators.cancel()

        // Assert: we reached Disconnected(error=TimeoutCancellationException)
        val state = presenter.wsClientConnectionState.value
        println("STATE=" + state)
        assertTrue(state is ConnectionState.Disconnected && state.error is TimeoutCancellationException)
    }
}

