package network.bisq.mobile.node.network.presentation.network

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.data.service.network.KmpTorService
import network.bisq.mobile.node.common.domain.service.network.NodeInfo
import network.bisq.mobile.node.common.domain.service.network.NodeNetworkServiceFacade
import network.bisq.mobile.node.common.presentation.navigation.NodeNavRoute
import network.bisq.mobile.node.common.test_utils.NodeKoinIntegrationTestBase
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
class NodeNetworkOverviewPresenterTest : NodeKoinIntegrationTestBase() {
    private val networkServiceFacade: NodeNetworkServiceFacade = mockk(relaxed = true)
    private val kmpTorService: KmpTorService = mockk(relaxed = true)
    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private val navigationManager: NavigationManager = mockk(relaxed = true)

    private val numConnections = MutableStateFlow(0)
    private val allDataReceived = MutableStateFlow(false)
    private val myNodeInfo = MutableStateFlow(NodeInfo())
    private val torState = MutableStateFlow<KmpTorService.TorState>(KmpTorService.TorState.Stopped())

    private lateinit var presenter: NodeNetworkOverviewPresenter

    // The floor's testModule binds a no-op NavigationManager mock; override it so the
    // navigation-dispatch tests can verify on this field mock.
    override fun additionalModules(): List<Module> = listOf(module { single<NavigationManager> { navigationManager } })

    override fun onSetup() {
        every { networkServiceFacade.numConnections } returns numConnections
        every { networkServiceFacade.allDataReceived } returns allDataReceived
        every { networkServiceFacade.myNodeInfo } returns myNodeInfo
        every { kmpTorService.state } returns torState
    }

    private fun createPresenter(): NodeNetworkOverviewPresenter =
        NodeNetworkOverviewPresenter(
            networkServiceFacade = networkServiceFacade,
            kmpTorService = kmpTorService,
            mainPresenter = mainPresenter,
        )

    @Test
    fun `when peers connected, tor started and data received then uiState is healthy`() =
        runTest {
            // Given
            numConnections.value = 5
            allDataReceived.value = true
            torState.value = KmpTorService.TorState.Started
            myNodeInfo.value = NodeInfo(onionAddress = "abcd.onion:1234")

            // When
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals(5, state.peerCount)
            assertTrue(state.isTorRunning)
            assertEquals("abcd.onion:1234", state.onionAddress)
            assertEquals(NetworkHealthState.HEALTHY, state.healthState)
        }

    @Test
    fun `when tor is not started then health is offline`() =
        runTest {
            // Given
            numConnections.value = 5
            allDataReceived.value = true
            torState.value = KmpTorService.TorState.Stopped()

            // When
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertFalse(state.isTorRunning)
            assertEquals(NetworkHealthState.OFFLINE, state.healthState)
        }

    @Test
    fun `when there are no peers then health is offline`() =
        runTest {
            // Given
            numConnections.value = 0
            allDataReceived.value = true
            torState.value = KmpTorService.TorState.Started

            // When
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals(0, state.peerCount)
            assertEquals(NetworkHealthState.OFFLINE, state.healthState)
        }

    @Test
    fun `when connected and tor running but data not received then health is syncing`() =
        runTest {
            // Given
            numConnections.value = 3
            allDataReceived.value = false
            torState.value = KmpTorService.TorState.Started

            // When
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // Then
            assertEquals(NetworkHealthState.SYNCING, presenter.uiState.value.healthState)
        }

    @Test
    fun `when numConnections is negative then peerCount is coerced to zero`() =
        runTest {
            // Given the facade reports -1 while the node is not yet available
            numConnections.value = -1
            torState.value = KmpTorService.TorState.Started
            allDataReceived.value = true

            // When
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals(0, state.peerCount)
            assertEquals(NetworkHealthState.OFFLINE, state.healthState)
        }

    @Test
    fun `when the network state changes then uiState recomputes reactively`() =
        runTest {
            // Given an attached presenter with an offline network
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()
            assertEquals(NetworkHealthState.OFFLINE, presenter.uiState.value.healthState)

            // When the node comes online
            numConnections.value = 8
            allDataReceived.value = true
            torState.value = KmpTorService.TorState.Started
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals(8, state.peerCount)
            assertEquals(NetworkHealthState.HEALTHY, state.healthState)
        }

    @Test
    fun `when connections action then navigates to peer connections`() =
        runTest {
            // Given
            presenter = createPresenter()

            // When
            presenter.onAction(NodeNetworkOverviewUiAction.OnConnectionsClick)
            advanceUntilIdle()

            // Then
            verify { navigationManager.navigate(NodeNavRoute.NetworkPeerConnections, any(), any()) }
        }

    @Test
    fun `when my node action then navigates to my node`() =
        runTest {
            // Given
            presenter = createPresenter()

            // When
            presenter.onAction(NodeNetworkOverviewUiAction.OnMyNodeClick)
            advanceUntilIdle()

            // Then
            verify { navigationManager.navigate(NodeNavRoute.NetworkMyNode, any(), any()) }
        }
}
