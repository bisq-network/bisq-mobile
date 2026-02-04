package network.bisq.mobile.client.trusted_node_setup

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepositoryMock
import network.bisq.mobile.client.common.domain.websocket.ConnectionState
import network.bisq.mobile.client.common.domain.websocket.WebSocketClient
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.test_utils.KoinIntegrationTestBase
import network.bisq.mobile.client.common.test_utils.UserRepositoryMock
import network.bisq.mobile.client.test_utils.TestDoubles
import network.bisq.mobile.domain.data.repository.UserRepository
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.network.KmpTorService
import network.bisq.mobile.presentation.main.MainPresenter
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import org.koin.core.module.Module
import org.koin.dsl.module

@ExperimentalCoroutinesApi
class TrustedNodeSetupPresenterTimeoutTest : KoinIntegrationTestBase() {
    private val wsClientService: WebSocketClientService = mockk(relaxed = true)
    private val kmpTorService: KmpTorService = mockk(relaxed = true)
    private val appBootstrap: ApplicationBootstrapFacade = mockk(relaxed = true)
    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private val sensitiveSettingsRepository: SensitiveSettingsRepository = SensitiveSettingsRepositoryMock()
    private val userRepository: UserRepository = UserRepositoryMock()

    override fun additionalModules(): List<Module> =
        listOf(
            module {
                single<WebSocketClientService> { wsClientService }
                single<KmpTorService> { kmpTorService }
                single<ApplicationBootstrapFacade> { appBootstrap }
            },
        )

    override fun onSetup() {
        // IMPORTANT: mock object before stubbing to avoid global leakage across tests
        mockkObject(WebSocketClient)

        // Mock timeout behavior
        every { wsClientService.connectionState } returns
            MutableStateFlow<ConnectionState>(
                ConnectionState.Disconnected(),
            )
        every { WebSocketClient.determineTimeout(any()) } returns 3_000L
        coEvery {
            wsClientService.testConnection(
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } coAnswers {
            // Simulate a timeout that returns the TimeoutCancellationException as a result
            val e =
                runCatching { withTimeout(1) { delay(50) } }.exceptionOrNull()
            e
        }
    }

    override fun onTearDown() {
        // Cleanup global mock to avoid leakage across tests
        TestDoubles.cleanupWebSocketClientMock()
    }

    @Ignore("Flaky under current test jobs manager; will enable after injecting test jobs manager here")
    @Test
    fun `timeout during connection is routed to error handler (not silent cancel)`() =
        runTest {
            val presenter =
                TrustedNodeSetupPresenter(
                    mainPresenter,
                    userRepository,
                    sensitiveSettingsRepository,
                    kmpTorService,
                    appBootstrap,
                )

            // Set valid inputs
            // Note: Proxy option is now automatically detected based on URL
            // No need to manually set proxy option or validate proxy URL

            // Act
            presenter.onTestAndSavePressed(isWorkflow = true)

            // Run all scheduled tasks/timeouts
            advanceUntilIdle()

            // Assert: we reached Disconnected(error=TimeoutCancellationException)
            val state = presenter.wsClientConnectionState.value
            println("STATE=" + state)
            assertTrue(state is ConnectionState.Disconnected && state.error is TimeoutCancellationException)
        }
}
