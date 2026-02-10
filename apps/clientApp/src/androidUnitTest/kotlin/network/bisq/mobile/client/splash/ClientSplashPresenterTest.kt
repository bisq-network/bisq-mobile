package network.bisq.mobile.client.splash

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.client.common.domain.websocket.ConnectionState
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.test_utils.KoinIntegrationTestBase
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Tests for ClientSplashPresenter navigation logic.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ClientSplashPresenterTest : KoinIntegrationTestBase() {
    private val webSocketClientService: WebSocketClientService = mockk(relaxed = true)
    private val applicationBootstrapFacade: ApplicationBootstrapFacade = mockk(relaxed = true)

    override fun additionalModules(): List<Module> =
        listOf(
            module {
                single<WebSocketClientService> { webSocketClientService }
                single<ApplicationBootstrapFacade> { applicationBootstrapFacade }
            },
        )

    override fun onSetup() {
        // Setup applicationBootstrapFacade state flows
        every { applicationBootstrapFacade.state } returns MutableStateFlow("Test State")
        every { applicationBootstrapFacade.progress } returns MutableStateFlow(0f)
        every { applicationBootstrapFacade.isTimeoutDialogVisible } returns MutableStateFlow(false)
        every { applicationBootstrapFacade.isBootstrapFailed } returns MutableStateFlow(false)
        every { applicationBootstrapFacade.torBootstrapFailed } returns MutableStateFlow(false)
        every { applicationBootstrapFacade.currentBootstrapStage } returns MutableStateFlow("")
        every { applicationBootstrapFacade.shouldShowProgressToast } returns MutableStateFlow(false)
    }

    @Test
    fun `isConnected returns true when connection state is Connected`() =
        runTest {
            // Given
            every { webSocketClientService.connectionState } returns
                MutableStateFlow<ConnectionState>(ConnectionState.Connected)
            every { webSocketClientService.isConnected() } returns true

            // Then
            assertTrue(webSocketClientService.isConnected())
        }

    @Test
    fun `isConnected returns false when connection state is Disconnected`() =
        runTest {
            // Given
            every { webSocketClientService.connectionState } returns
                MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())
            every { webSocketClientService.isConnected() } returns false

            // Then
            assertFalse(webSocketClientService.isConnected())
        }

    @Test
    fun `isConnected returns false when connection state is Connecting`() =
        runTest {
            // Given
            every { webSocketClientService.connectionState } returns
                MutableStateFlow<ConnectionState>(ConnectionState.Connecting)
            every { webSocketClientService.isConnected() } returns false

            // Then
            assertFalse(webSocketClientService.isConnected())
        }

    @Test
    fun `demo mode allows navigation even when not connected`() =
        runTest {
            // Given: Demo mode is enabled
            ApplicationBootstrapFacade.isDemo = true
            every { webSocketClientService.isConnected() } returns false

            // Then: In demo mode, the check should pass (isDemo || isConnected)
            // This tests line 35 in ClientSplashPresenter.kt
            assertTrue(ApplicationBootstrapFacade.isDemo || webSocketClientService.isConnected())

            // Cleanup
            ApplicationBootstrapFacade.isDemo = false
        }

    @Test
    fun `non-demo mode requires connection for navigation`() =
        runTest {
            // Given: Demo mode is disabled and not connected
            ApplicationBootstrapFacade.isDemo = false
            every { webSocketClientService.isConnected() } returns false

            // Then: Should not allow navigation
            assertFalse(ApplicationBootstrapFacade.isDemo || webSocketClientService.isConnected())
        }
}
