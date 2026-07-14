package network.bisq.mobile.node.network.presentation.connections

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.DefaultCoroutineJobsManager
import network.bisq.mobile.node.common.domain.service.network.NodeNetworkServiceFacade
import network.bisq.mobile.node.common.domain.service.network.NodePeerInfo
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

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkConnectionsPresenterTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var networkServiceFacade: NodeNetworkServiceFacade
    private lateinit var mainPresenter: MainPresenter
    private lateinit var globalUiManager: GlobalUiManager
    private lateinit var navigationManager: NavigationManager

    private lateinit var connectedPeers: MutableStateFlow<List<NodePeerInfo>>

    private lateinit var presenter: NetworkConnectionsPresenter

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        networkServiceFacade = mockk(relaxed = true)
        mainPresenter = mockk(relaxed = true)
        globalUiManager = mockk(relaxed = true)
        navigationManager = mockk(relaxed = true)

        connectedPeers = MutableStateFlow(emptyList())
        every { networkServiceFacade.connectedPeers } returns connectedPeers

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

    private fun createPresenter(): NetworkConnectionsPresenter =
        NetworkConnectionsPresenter(
            networkServiceFacade = networkServiceFacade,
            mainPresenter = mainPresenter,
        )

    @Test
    fun `when peers are present then uiState exposes count and peers`() =
        runTest(testDispatcher) {
            // Given
            connectedPeers.value =
                listOf(
                    samplePeer(connectionId = "a", establishedAtMillis = 100L),
                    samplePeer(connectionId = "b", establishedAtMillis = 200L),
                )

            // When
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals(2, state.peerCount)
            assertEquals(2, state.peers.size)
        }

    @Test
    fun `when peers are empty then uiState is empty`() =
        runTest(testDispatcher) {
            // Given no peers

            // When
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals(0, state.peerCount)
            assertEquals(emptyList(), state.peers)
        }

    @Test
    fun `when peers arrive unordered then newest established peers come first`() =
        runTest(testDispatcher) {
            // Given peers in mixed establishment order
            connectedPeers.value =
                listOf(
                    samplePeer(connectionId = "early", establishedAtMillis = 100L),
                    samplePeer(connectionId = "late", establishedAtMillis = 300L),
                    samplePeer(connectionId = "mid", establishedAtMillis = 200L),
                )

            // When
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // Then newest (highest establishedAtMillis) first
            assertEquals(
                listOf("late", "mid", "early"),
                presenter.uiState.value.peers
                    .map { it.connectionId },
            )
        }

    @Test
    fun `when the peer list changes then uiState updates reactively`() =
        runTest(testDispatcher) {
            // Given an attached presenter with no peers
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()
            assertEquals(0, presenter.uiState.value.peerCount)

            // When a peer connects
            connectedPeers.value = listOf(samplePeer(connectionId = "a"))
            advanceUntilIdle()

            // Then
            assertEquals(1, presenter.uiState.value.peerCount)
        }

    private fun samplePeer(
        connectionId: String = "id",
        address: String = "abcd.onion:1234",
        isOutbound: Boolean = true,
        establishedAtMillis: Long = 0L,
        isSeed: Boolean = false,
    ): NodePeerInfo =
        NodePeerInfo(
            connectionId = connectionId,
            address = address,
            isOutbound = isOutbound,
            establishedAtMillis = establishedAtMillis,
            isSeed = isSeed,
        )
}
