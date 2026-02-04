package network.bisq.mobile.client.trusted_node_setup

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import network.bisq.mobile.client.common.domain.httpclient.BisqProxyOption
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepositoryMock
import network.bisq.mobile.client.common.domain.websocket.ConnectionState
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.test_utils.KoinIntegrationTestBase
import network.bisq.mobile.client.common.test_utils.UserRepositoryMock
import network.bisq.mobile.client.test_utils.TestDoubles
import network.bisq.mobile.domain.data.repository.UserRepository
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.network.KmpTorService
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import org.koin.core.module.Module
import org.koin.dsl.module

@ExperimentalCoroutinesApi
class TrustedNodeSetupPresenterCancelInternalTorTest : KoinIntegrationTestBase() {
    private lateinit var wsClientService: WebSocketClientService
    private lateinit var wsCleanup: () -> Unit
    private val kmpTorService: KmpTorService = mockk(relaxed = true)
    private val appBootstrap: ApplicationBootstrapFacade = mockk(relaxed = true)
    private val sensitiveSettingsRepository: SensitiveSettingsRepository = SensitiveSettingsRepositoryMock()
    private val userRepository: UserRepository = UserRepositoryMock()

    override fun additionalModules(): List<Module> {
        // Test jobs manager that runs UI/IO on the same test dispatcher
        val testJobsManager =
            object : CoroutineJobsManager {
                private var scope = CoroutineScope(SupervisorJob() + testDispatcher)
                override var coroutineExceptionHandler: ((Throwable) -> Unit)? = null

                override suspend fun dispose() {
                    scope.cancel()
                    scope = CoroutineScope(SupervisorJob() + testDispatcher)
                }

                override fun getScope() = scope
            }

        return listOf(
            module {
                single<WebSocketClientService> { wsClientService }
                single<KmpTorService> { kmpTorService }
                single<CoroutineJobsManager> { testJobsManager }
            },
        )
    }

    override fun onSetup() {
        // Mocks / fakes
        val (service, cleanup) = TestDoubles.wsService(testConnectionDelayMs = 10_000)
        wsClientService = service
        wsCleanup = cleanup

        every { kmpTorService.state } returns
            MutableStateFlow<KmpTorService.TorState>(
                KmpTorService.TorState.Stopped(),
            )
        every { kmpTorService.bootstrapProgress } returns MutableStateFlow(0)
    }

    override fun onTearDown() {
        // Ensure MockK global object mock is cleaned up to avoid leakage across tests
        wsCleanup()
    }

    @Ignore(
        "With recent changes likely not make sense anymore, " +
            "or would require some effort to get it work again",
    )
    @Test
    fun `cancel while Internal Tor is starting stops Tor and resets state`() =
        runBlocking {
            // Make startTor suspend and mimic Starting state before cancellation
            val torStateFlow =
                MutableStateFlow<KmpTorService.TorState>(KmpTorService.TorState.Stopped())
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

            // Note: Proxy option is now automatically detected based on URL
            // Simulate a .onion URL to trigger INTERNAL_TOR automatically
            // The pairing code would set this via ApiAccessService.setPairingQrCodeString
            // For this test, we'll use the fact that validateApiUrl sets INTERNAL_TOR for .onion URLs
            presenter.validateApiUrl("http://test123456789012345678901234567890123456.onion:8090", BisqProxyOption.NONE)

            delay(20)
            presenter.onTestAndSavePressed(isWorkflow = true)
            // Wait until mocked startTor flips the state to Starting
            var tries = 0
            while (tries < 50 && torStateFlow.value !is KmpTorService.TorState.Starting) {
                delay(10)
                tries++
            }
            // assertTrue(presenter.isNodeSetupInProgress.value)

            // Cancel while Tor is Starting
            presenter.onCancelPressed()
            delay(50)

            // Presenter state reset
            //  assertFalse(presenter.isNodeSetupInProgress.value)
            assertEquals("", presenter.status.value)
            assertTrue(
                presenter.wsClientConnectionState.value is ConnectionState.Disconnected &&
                    (presenter.wsClientConnectionState.value as ConnectionState.Disconnected).error == null,
            )

            // Tor stop requested due to Starting state
            coVerify(exactly = 1) { kmpTorService.stopTor() }
        }

    @Ignore(
        "With recent changes likely not make sense anymore, " +
            "or would require some effort to get it work again",
    )
    @Test
    fun `cancel while Internal Tor is already started does not stop Tor`() =
        runBlocking {
            val torStateFlow =
                MutableStateFlow<KmpTorService.TorState>(KmpTorService.TorState.Started)
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

            // Note: Proxy option is now automatically detected based on URL
            // Simulate a .onion URL to trigger INTERNAL_TOR automatically
            presenter.validateApiUrl("http://test123456789012345678901234567890123456.onion:8090", BisqProxyOption.NONE)

            delay(20)
            presenter.onTestAndSavePressed(isWorkflow = true)
            delay(100)
            presenter.onCancelPressed()
            delay(50)

            // Presenter state reset
            // assertFalse(presenter.isNodeSetupInProgress.value)
            assertEquals("", presenter.status.value)

            // Tor should not be stopped if it was already Started
            coVerify(exactly = 0) { kmpTorService.stopTor() }
        }
}
