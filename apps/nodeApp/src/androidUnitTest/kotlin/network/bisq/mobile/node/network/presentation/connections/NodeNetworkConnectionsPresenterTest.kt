package network.bisq.mobile.node.network.presentation.connections

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.node.common.domain.service.network.NodeInfo
import network.bisq.mobile.node.common.domain.service.network.NodeNetworkServiceFacade
import network.bisq.mobile.node.common.domain.service.network.NodePeerInfo
import network.bisq.mobile.node.common.test_utils.NodeKoinIntegrationTestBase
import network.bisq.mobile.presentation.main.MainPresenter
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class NodeNetworkConnectionsPresenterTest : NodeKoinIntegrationTestBase() {
    private val networkServiceFacade: NodeNetworkServiceFacade = mockk(relaxed = true)
    private val mainPresenter: MainPresenter = mockk(relaxed = true)

    private val connectedPeers = MutableStateFlow<List<NodePeerInfo>>(emptyList())
    private val myNodeInfo = MutableStateFlow(NodeInfo())

    private lateinit var presenter: NodeNetworkConnectionsPresenter

    override fun onSetup() {
        every { networkServiceFacade.connectedPeers } returns connectedPeers
        every { networkServiceFacade.myNodeInfo } returns myNodeInfo
    }

    private fun createPresenter(): NodeNetworkConnectionsPresenter =
        NodeNetworkConnectionsPresenter(
            networkServiceFacade = networkServiceFacade,
            mainPresenter = mainPresenter,
        )

    @Test
    fun `when peers are present then uiState exposes count and peers`() =
        runTest {
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
        runTest {
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
        runTest {
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
        runTest {
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

    @Test
    fun `when node identity resolves then uiState exposes keyId and nodeTag`() =
        runTest {
            // Given
            myNodeInfo.value = NodeInfo(keyId = "key-123", nodeTag = "default")

            // When
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals("key-123", state.keyId)
            assertEquals("default", state.nodeTag)
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
            rttMillis = null,
            sentBytes = 0L,
            sentMessageCount = 0L,
            receivedBytes = 0L,
            receivedMessageCount = 0L,
        )
}
