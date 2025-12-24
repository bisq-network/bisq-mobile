package network.bisq.mobile.client.trusted_node_setup

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.test_utils.TestDoubles
import network.bisq.mobile.domain.data.model.User
import network.bisq.mobile.domain.data.repository.UserRepository
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.network.KmpTorService
import network.bisq.mobile.domain.utils.CoroutineJobsManager
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

@ExperimentalCoroutinesApi
class TrustedNodeSetupPresenterCancelInternalTorTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var wsClientService: WebSocketClientService
    private lateinit var kmpTorService: KmpTorService
    private lateinit var appBootstrap: ApplicationBootstrapFacade
    private lateinit var mainPresenter: MainPresenter

    private lateinit var sensitiveSettingsRepository: SensitiveSettingsRepository
    private lateinit var userRepository: UserRepository

    private lateinit var wsCleanup: () -> Unit

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Mocks / fakes
        val (service, cleanup) = TestDoubles.wsService(testConnectionDelayMs = 10_000)
        wsClientService = service
        wsCleanup = cleanup
        kmpTorService = mockk(relaxed = true)
        appBootstrap = mockk(relaxed = true)

        every { kmpTorService.state } returns MutableStateFlow<KmpTorService.TorState>(KmpTorService.TorState.Stopped())
        every { kmpTorService.bootstrapProgress } returns MutableStateFlow(0)

        // Minimal repositories
        sensitiveSettingsRepository =
            object : SensitiveSettingsRepository {
                private val _data = MutableStateFlow(SensitiveSettings())
                override val data = _data

                override suspend fun fetch() = _data.value

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

        // Test jobs manager that runs UI/IO on the same test dispatcher
        val testJobsManager =
            object : CoroutineJobsManager {
                private val scope = CoroutineScope(testDispatcher)

                override suspend fun dispose() {}

                override fun getScope() = scope

                override fun setCoroutineExceptionHandler(handler: (Throwable) -> Unit) {}
            }

        startKoin {
            modules(
                clientTestModule,
                module {
                    single<WebSocketClientService> { wsClientService }
                    single<KmpTorService> { kmpTorService }
                    single<CoroutineJobsManager> { testJobsManager }
                },
            )
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
        // Ensure MockK global object mock is cleaned up to avoid leakage across tests
        wsCleanup()
    }

    @Test
    fun `cancel while Internal Tor is starting stops Tor and resets state`() =
        runBlocking {
            // Make startTor suspend and mimic Starting state before cancellation
            val torStateFlow = MutableStateFlow<KmpTorService.TorState>(KmpTorService.TorState.Stopped())
            every { kmpTorService.state } returns torStateFlow
            coEvery { kmpTorService.startTor(any()) } coAnswers {
                torStateFlow.value = KmpTorService.TorState.Starting
                delay(10_000)
                true
            }

            val presenter =
                TrustedNodeSetupPresenter(
                    mainPresenter = mockk(relaxed = true),
                    userRepository,
                    sensitiveSettingsRepository,
                    kmpTorService,
                    appBootstrap,
                )

            presenter.onProxyOptionChanged(BisqProxyOption.INTERNAL_TOR)
            presenter.onApiUrlChanged("http://127.0.0.1:8090")

            // Start validators (lazy flows) so onTestAndSavePressed will proceed
            val validators =
                CoroutineScope(testDispatcher).launch {
                    launch { presenter.isApiUrlValid.collect { } }
                    launch { presenter.isProxyUrlValid.collect { } }
                }
            delay(20)
            presenter.onTestAndSavePressed(isWorkflow = true)
            // Wait until mocked startTor flips the state to Starting
            var tries = 0
            while (tries < 50 && torStateFlow.value !is KmpTorService.TorState.Starting) {
                delay(10)
                tries++
            }
            assertTrue(presenter.isNodeSetupInProgress.value)

            // Cancel while Tor is Starting
            presenter.onCancelPressed()
            delay(50)

            // Presenter state reset
            assertFalse(presenter.isNodeSetupInProgress.value)
            assertEquals("", presenter.status.value)
            assertTrue(
                presenter.wsClientConnectionState.value is ConnectionState.Disconnected &&
                    (presenter.wsClientConnectionState.value as ConnectionState.Disconnected).error == null,
            )

            // Tor stop requested due to Starting state
            coVerify(exactly = 1) { kmpTorService.stopTor() }
        }

    @Test
    fun `cancel while Internal Tor is already started does not stop Tor`() =
        runBlocking {
            val torStateFlow = MutableStateFlow<KmpTorService.TorState>(KmpTorService.TorState.Started)
            every { kmpTorService.state } returns torStateFlow
            coEvery { kmpTorService.startTor(any()) } returns true
            coEvery { kmpTorService.awaitSocksPort() } returns 9050

            val presenter =
                TrustedNodeSetupPresenter(
                    mainPresenter = mockk(relaxed = true),
                    userRepository,
                    sensitiveSettingsRepository,
                    kmpTorService,
                    appBootstrap,
                )

            presenter.onProxyOptionChanged(BisqProxyOption.INTERNAL_TOR)
            presenter.onApiUrlChanged("http://127.0.0.1:8090")

            val validators =
                CoroutineScope(testDispatcher).launch {
                    launch { presenter.isApiUrlValid.collect { } }
                    launch { presenter.isProxyUrlValid.collect { } }
                }
            delay(20)
            presenter.onTestAndSavePressed(isWorkflow = true)
            delay(100)
            presenter.onCancelPressed()
            delay(50)
            validators.cancel()

            // Presenter state reset
            assertFalse(presenter.isNodeSetupInProgress.value)
            assertEquals("", presenter.status.value)

            // Tor should not be stopped if it was already Started
            coVerify(exactly = 0) { kmpTorService.stopTor() }
        }
}
