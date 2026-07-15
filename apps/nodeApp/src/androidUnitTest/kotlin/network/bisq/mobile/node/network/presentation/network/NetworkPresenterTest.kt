package network.bisq.mobile.node.network.presentation.network

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.data.service.network.KmpTorService
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.DefaultCoroutineJobsManager
import network.bisq.mobile.node.common.domain.service.network.NodeInfo
import network.bisq.mobile.node.common.domain.service.network.NodeNetworkServiceFacade
import network.bisq.mobile.node.common.presentation.navigation.NodeNavRoute
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.main.MainPresenter
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkPresenterTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var networkServiceFacade: NodeNetworkServiceFacade
    private lateinit var kmpTorService: KmpTorService
    private lateinit var mainPresenter: MainPresenter
    private lateinit var globalUiManager: GlobalUiManager
    private lateinit var navigationManager: NavigationManager

    private lateinit var numConnections: MutableStateFlow<Int>
    private lateinit var allDataReceived: MutableStateFlow<Boolean>
    private lateinit var myNodeInfo: MutableStateFlow<NodeInfo>
    private lateinit var torState: MutableStateFlow<KmpTorService.TorState>

    private lateinit var presenter: NetworkPresenter

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        networkServiceFacade = mockk(relaxed = true)
        kmpTorService = mockk(relaxed = true)
        mainPresenter = mockk(relaxed = true)
        globalUiManager = mockk(relaxed = true)
        navigationManager = mockk(relaxed = true)

        numConnections = MutableStateFlow(0)
        allDataReceived = MutableStateFlow(false)
        myNodeInfo = MutableStateFlow(NodeInfo())
        torState = MutableStateFlow(KmpTorService.TorState.Stopped())

        every { networkServiceFacade.numConnections } returns numConnections
        every { networkServiceFacade.allDataReceived } returns allDataReceived
        every { networkServiceFacade.myNodeInfo } returns myNodeInfo
        every { kmpTorService.state } returns torState

        startKoin {
            modules(
                module {
                    single<NavigationManager> { navigationManager }
                    single<CoroutineJobsManager> { DefaultCoroutineJobsManager() }
                    single<GlobalUiManager> { globalUiManager }
                },
            )
        }
    }

    @AfterTest
    fun tearDown() {
        try {
            stopKoin()
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun createPresenter(): NetworkPresenter =
        NetworkPresenter(
            networkServiceFacade = networkServiceFacade,
            kmpTorService = kmpTorService,
            mainPresenter = mainPresenter,
        )

    @Test
    fun `when peers connected, tor started and data received then uiState is healthy`() =
        runTest(testDispatcher) {
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
        runTest(testDispatcher) {
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
        runTest(testDispatcher) {
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
        runTest(testDispatcher) {
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
        runTest(testDispatcher) {
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
        runTest(testDispatcher) {
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
        runTest(testDispatcher) {
            // Given
            presenter = createPresenter()

            // When
            presenter.onAction(NetworkUiAction.OnConnectionsClick)
            advanceUntilIdle()

            // Then
            verify { navigationManager.navigate(NodeNavRoute.NetworkPeerConnections, any(), any()) }
        }

    @Test
    fun `when my node action then navigates to my node`() =
        runTest(testDispatcher) {
            // Given
            presenter = createPresenter()

            // When
            presenter.onAction(NetworkUiAction.OnMyNodeClick)
            advanceUntilIdle()

            // Then
            verify { navigationManager.navigate(NodeNavRoute.NetworkMyNode, any(), any()) }
        }
}
