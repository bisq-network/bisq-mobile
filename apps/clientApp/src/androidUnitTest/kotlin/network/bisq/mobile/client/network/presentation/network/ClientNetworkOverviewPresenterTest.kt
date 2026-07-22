package network.bisq.mobile.client.network.presentation.network

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.client.common.domain.httpclient.BisqProxyOption
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettings
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import network.bisq.mobile.client.common.domain.service.network.ClientConnectivityService
import network.bisq.mobile.client.common.domain.service.network.ClientNetworkServiceFacade
import network.bisq.mobile.client.common.domain.service.network.ConnectionDto
import network.bisq.mobile.client.common.domain.service.network.NetworkInfoDto
import network.bisq.mobile.client.common.presentation.navigation.ClientNavRoute
import network.bisq.mobile.client.common.test_utils.ClientKoinIntegrationTestBase
import network.bisq.mobile.data.service.network.ConnectivityService.ConnectivityStatus
import network.bisq.mobile.presentation.common.ui.components.network.NetworkHealthState
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.main.MainPresenter
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ClientNetworkOverviewPresenterTest : ClientKoinIntegrationTestBase() {
    private val networkServiceFacade: ClientNetworkServiceFacade = mockk(relaxed = true)
    private val sensitiveSettingsRepository: SensitiveSettingsRepository = mockk(relaxed = true)
    private val connectivityService: ClientConnectivityService = mockk(relaxed = true)
    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private val navigationManager: NavigationManager = mockk(relaxed = true)

    private val status = MutableStateFlow(ConnectivityStatus.DISCONNECTED)
    private val networkInfo = MutableStateFlow<NetworkInfoDto?>(null)

    private lateinit var presenter: ClientNetworkOverviewPresenter

    // The base's clientTestModule binds a no-op NavigationManager stub; override it so the
    // navigation-dispatch tests can verify on a mock.
    override fun additionalModules(): List<Module> = listOf(module { single<NavigationManager> { navigationManager } })

    override fun onSetup() {
        every { connectivityService.status } returns status
        every { networkServiceFacade.networkInfo } returns networkInfo
        every { connectivityService.currentAverageRoundTripTimeMs() } returns -1L
        coEvery { sensitiveSettingsRepository.fetch() } returns
            SensitiveSettings(
                bisqApiUrl = "http://r7m2xpqowg3bvf8t.onion:8090",
                selectedProxyOption = BisqProxyOption.INTERNAL_TOR,
            )
    }

    private fun createPresenter(): ClientNetworkOverviewPresenter =
        ClientNetworkOverviewPresenter(
            networkServiceFacade = networkServiceFacade,
            sensitiveSettingsRepository = sensitiveSettingsRepository,
            connectivityService = connectivityService,
            mainPresenter = mainPresenter,
        )

    // The peer count is derived from connections.size, so build a list of the desired size.
    private fun networkInfoWith(
        connectionCount: Int,
        allDataReceived: Boolean = true,
        torRunning: Boolean = true,
    ): NetworkInfoDto =
        NetworkInfoDto(
            allDataReceived = allDataReceived,
            torRunning = torRunning,
            connections =
                List(connectionCount) { i ->
                    ConnectionDto(
                        connectionId = "c$i",
                        address = "peer$i.onion",
                        outbound = true,
                        seed = false,
                        establishedAtMillis = 0L,
                    )
                },
        )

    @Test
    fun `when connected and data received then uiState is healthy with peer count and host`() =
        runTest {
            // Given
            status.value = ConnectivityStatus.CONNECTED_AND_DATA_RECEIVED
            networkInfo.value = networkInfoWith(connectionCount = 12)
            every { connectivityService.currentAverageRoundTripTimeMs() } returns 340L

            // When
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertTrue(state.isReachable)
            assertEquals(12, state.peerCountViaNode)
            assertEquals("r7m2xpqowg3bvf8t.onion", state.trustedNodeHost)
            assertTrue(state.isTorRouted)
            assertEquals(340L, state.latencyMs)
            assertEquals(NetworkHealthState.HEALTHY, state.healthState)
        }

    @Test
    fun `when connected and data received but zero peers then health is offline`() =
        runTest {
            // Given the node is reachable and synced but reports no peers (no mesh connectivity)
            status.value = ConnectivityStatus.CONNECTED_AND_DATA_RECEIVED
            networkInfo.value = networkInfoWith(connectionCount = 0, allDataReceived = true)

            // When
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // Then — mirrors the node presenter: 0 peers is OFFLINE, not HEALTHY
            val state = presenter.uiState.value
            assertTrue(state.isReachable)
            assertEquals(0, state.peerCountViaNode)
            assertEquals(NetworkHealthState.OFFLINE, state.healthState)
        }

    @Test
    fun `when round-trip time is the sentinel then latency is null`() =
        runTest {
            // Given no request round-trip has completed yet
            status.value = ConnectivityStatus.CONNECTED_AND_DATA_RECEIVED
            every { connectivityService.currentAverageRoundTripTimeMs() } returns -1L

            // When
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // Then latency is reported as "not measured yet"
            assertEquals(null, presenter.uiState.value.latencyMs)
        }

    @Test
    fun `when disconnected then health is offline`() =
        runTest {
            // Given connectivity is down (e.g. airplane mode: health checks fail → DISCONNECTED)
            status.value = ConnectivityStatus.DISCONNECTED

            // When
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertFalse(state.isReachable)
            assertEquals(NetworkHealthState.OFFLINE, state.healthState)
        }

    @Test
    fun `when reachable but no network info yet then health is syncing`() =
        runTest {
            // Given the link is up but the trusted node has not pushed a snapshot yet
            status.value = ConnectivityStatus.CONNECTED_AND_DATA_RECEIVED
            networkInfo.value = null

            // When
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertTrue(state.isReachable)
            assertEquals(null, state.peerCountViaNode)
            assertEquals(NetworkHealthState.SYNCING, state.healthState)
        }

    @Test
    fun `when the network state changes then uiState recomputes reactively`() =
        runTest {
            // Given an attached presenter with an offline link
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()
            assertEquals(NetworkHealthState.OFFLINE, presenter.uiState.value.healthState)

            // When the link comes up and the node pushes data
            status.value = ConnectivityStatus.CONNECTED_AND_DATA_RECEIVED
            networkInfo.value = networkInfoWith(connectionCount = 8)
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals(8, state.peerCountViaNode)
            assertEquals(NetworkHealthState.HEALTHY, state.healthState)
        }

    @Test
    fun `when the link drops then the stale peer count is cleared`() =
        runTest {
            // Given a reachable link with a pushed snapshot
            status.value = ConnectivityStatus.CONNECTED_AND_DATA_RECEIVED
            networkInfo.value = networkInfoWith(connectionCount = 8)
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()
            assertEquals(8, presenter.uiState.value.peerCountViaNode)

            // When connectivity drops (the last snapshot is not cleared by the facade)
            status.value = ConnectivityStatus.DISCONNECTED
            advanceUntilIdle()

            // Then the peer count is not shown as stale under the OFFLINE badge
            val state = presenter.uiState.value
            assertEquals(null, state.peerCountViaNode)
            assertEquals(NetworkHealthState.OFFLINE, state.healthState)
        }

    @Test
    fun `when connections action then navigates to network connections`() =
        runTest {
            presenter = createPresenter()

            presenter.onAction(ClientNetworkOverviewUiAction.OnConnectionsClick)
            advanceUntilIdle()

            verify { navigationManager.navigate(ClientNavRoute.NetworkConnections, any(), any()) }
        }

    @Test
    fun `when my connection action then navigates to my connection`() =
        runTest {
            presenter = createPresenter()

            presenter.onAction(ClientNetworkOverviewUiAction.OnMyConnectionClick)
            advanceUntilIdle()

            verify { navigationManager.navigate(ClientNavRoute.NetworkMyConnection, any(), any()) }
        }

    @Test
    fun `when check settings action then navigates to trusted node setup settings`() =
        runTest {
            presenter = createPresenter()

            presenter.onAction(ClientNetworkOverviewUiAction.OnCheckConnectionSettings)
            advanceUntilIdle()

            verify { navigationManager.navigate(ClientNavRoute.TrustedNodeSetupSettings, any(), any()) }
        }
}
