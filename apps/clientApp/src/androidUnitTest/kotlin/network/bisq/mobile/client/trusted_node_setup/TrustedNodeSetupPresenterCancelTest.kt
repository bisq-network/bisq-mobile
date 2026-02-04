package network.bisq.mobile.client.trusted_node_setup

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import org.koin.core.module.Module
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
class TrustedNodeSetupPresenterCancelTest : KoinIntegrationTestBase() {
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
            },
        )

    override fun onSetup() {
        // Mock timeout and connection: delay long so we can cancel
        // IMPORTANT: mock object before stubbing to avoid global leakage across tests
        mockkObject(WebSocketClient)
        every { wsClientService.connectionState } returns
            MutableStateFlow<ConnectionState>(
                ConnectionState.Disconnected(),
            )
        every { WebSocketClient.determineTimeout(any()) } returns 60_000L
        coEvery {
            wsClientService.testConnection(
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } coAnswers {
            delay(10_000)
            null
        }
    }

    override fun onTearDown() {
        // Cleanup global mock to avoid leakage across tests
        TestDoubles.cleanupWebSocketClientMock()
    }

    @Ignore(
        "With recent changes likely not make sense anymore, " +
            "or would require some effort to get it work again",
    )
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
            // Note: Proxy option is now automatically detected based on URL
            // No need to manually set proxy option or validate proxy URL

            // Allow any initialization to complete
            delay(50)
            // Start connection
            presenter.onTestAndSavePressed(isWorkflow = true)
            // Let things start
            delay(100)
            // assertTrue(presenter.isNodeSetupInProgress.value)
            assertTrue(presenter.wsClientConnectionState.value is ConnectionState.Connecting)

            // Cancel
            presenter.onCancelPressed()
            // Give coroutine a beat to process cancellation
            delay(50)

            // Assert state reset
            // assertFalse(presenter.isNodeSetupInProgress.value)
            assertEquals("", presenter.status.value)
            val state = presenter.wsClientConnectionState.value
            assertTrue(state is ConnectionState.Disconnected && state.error == null)
            assertEquals(0L, presenter.timeoutCounter.value)

            // Ensure we never proceeded to connect() or disposeClient() after cancel
            coVerify(exactly = 0) { wsClientService.connect() }
            coVerify(exactly = 0) { wsClientService.disposeClient() }
        }
}
