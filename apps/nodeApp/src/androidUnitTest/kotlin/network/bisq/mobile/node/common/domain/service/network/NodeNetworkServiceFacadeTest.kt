package network.bisq.mobile.node.common.domain.service.network

import bisq.common.network.TransportType
import bisq.common.observable.Observable
import bisq.network.identity.NetworkId
import bisq.network.p2p.ServiceNode
import bisq.network.p2p.message.EnvelopePayloadMessage
import bisq.network.p2p.node.Connection
import bisq.network.p2p.node.Node
import bisq.network.p2p.node.network_load.ConnectionMetrics
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService
import network.bisq.mobile.node.common.test_utils.NodeKoinIntegrationTestBase
import network.bisq.mobile.test.coroutines.StandardTestDispatcherProvider
import org.junit.Test
import java.util.Optional
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers the peer-state refresh lifecycle: a single collector coroutine owns every write, the sampled
 * metrics arm only runs while something collects [NodeNetworkServiceFacade.connectedPeers], the
 * connect/disconnect arm stays live regardless (it feeds the network status indicator), and a snapshot
 * that throws (bisq2 metrics race, #1796) does not end the collector.
 */
class NodeNetworkServiceFacadeTest : NodeKoinIntegrationTestBase() {
    // Comfortably past METRICS_REFRESH_INTERVAL_MS so a sampled tick has certainly had its chance.
    private val pastRefreshInterval = 10_000L

    private lateinit var applicationService: AndroidApplicationService
    private lateinit var provider: AndroidApplicationService.Provider
    private lateinit var serviceNode: ServiceNode
    private lateinit var node: Node
    private lateinit var connection: Connection
    private lateinit var secondConnection: Connection
    private lateinit var metrics: ConnectionMetrics
    private lateinit var serviceNodeState: Observable<ServiceNode.State>

    private lateinit var facade: NodeNetworkServiceFacade

    override fun onSetup() {
        metrics = connectionMetrics(sentBytes = 100L)
        connection = connection(id = "conn-1", metrics = metrics)
        secondConnection = connection(id = "conn-2", metrics = connectionMetrics(sentBytes = 50L))

        node = mockk(relaxed = true)
        every { node.networkId } returns mockk<NetworkId>(relaxed = true)
        every { node.findMyAddress() } returns Optional.empty()
        every { node.numConnections } returns 1
        // answers, not returns: a Stream is single-use and every refresh consumes one.
        every { node.allActiveConnections } answers { Stream.of(connection) }

        // A real Observable the test publishes through, so activation runs the production observer.
        serviceNodeState = Observable()
        serviceNode = mockk(relaxed = true)
        every { serviceNode.getState() } returns serviceNodeState
        every { serviceNode.defaultNode } returns node
        every { serviceNode.peerGroupManager } returns Optional.empty()
        every { serviceNode.inventoryService } returns Optional.empty()

        applicationService = mockk(relaxed = true)
        every {
            applicationService.networkService.serviceNodesByTransport.serviceNodesByTransport
        } returns mapOf(TransportType.CLEAR to serviceNode)
        every { applicationService.identityService.findAnyIdentityByNetworkId(any()) } returns Optional.empty()

        provider = AndroidApplicationService.Provider().apply { applicationService = this@NodeNetworkServiceFacadeTest.applicationService }

        facade =
            NodeNetworkServiceFacade(
                provider = provider,
                kmpTorService = mockk(relaxed = true),
                applicationBootstrapFacade = mockk(relaxed = true),
                dispatcherProvider = StandardTestDispatcherProvider(testDispatcher),
            )
    }

    @Test
    fun `when nothing collects connectedPeers then inbound traffic does not refresh metrics`() =
        runTest {
            // Given an activated facade holding the initial snapshot
            activateFacade()
            assertEquals(
                100L,
                facade.connectedPeers.value
                    .single()
                    .sentBytes,
            )

            // When the peer's counters move and traffic arrives, but no screen is attached
            every { metrics.sentBytes } returns 999L
            facade.onMessage(mockk(relaxed = true), connection, mockk(relaxed = true))
            advanceTimeBy(pastRefreshInterval)
            advanceUntilIdle()

            // Then the node did no metrics work — the snapshot is untouched
            assertEquals(
                100L,
                facade.connectedPeers.value
                    .single()
                    .sentBytes,
            )
        }

    @Test
    fun `when a collector attaches then metrics refresh immediately`() =
        runTest {
            // Given counters that moved while nothing was watching
            activateFacade()
            every { metrics.sentBytes } returns 999L

            // When a screen starts collecting
            // runCurrent, not advanceUntilIdle: once the gate opens, sample() keeps a ticker scheduled
            // forever, so advanceUntilIdle would spin advancing virtual time and never return.
            backgroundScope.launch { facade.connectedPeers.collect { } }
            runCurrent()

            // Then it opens on fresh counters rather than the stale snapshot
            assertEquals(
                999L,
                facade.connectedPeers.value
                    .single()
                    .sentBytes,
            )
        }

    @Test
    fun `when a collector is attached then inbound traffic refreshes metrics on the sampled cadence`() =
        runTest {
            // Given an attached collector (see the runCurrent note above — sample() never goes idle)
            activateFacade()
            backgroundScope.launch { facade.connectedPeers.collect { } }
            runCurrent()
            // The attach refresh has already captured the original counters, so the assertion below can
            // only pass via the sampled tick.
            assertEquals(
                100L,
                facade.connectedPeers.value
                    .single()
                    .sentBytes,
            )

            // When counters move and traffic arrives
            every { metrics.sentBytes } returns 999L
            facade.onMessage(mockk(relaxed = true), connection, mockk(relaxed = true))
            advanceTimeBy(pastRefreshInterval)
            runCurrent()

            // Then the refresh picks them up
            assertEquals(
                999L,
                facade.connectedPeers.value
                    .single()
                    .sentBytes,
            )
        }

    @Test
    fun `when a peer connects then the list updates even with no collector attached`() =
        runTest {
            // Given an activated facade with one peer and nothing collecting
            activateFacade()
            assertEquals(1, facade.connectedPeers.value.size)

            // When a second peer connects
            every { node.allActiveConnections } answers { Stream.of(connection, secondConnection) }
            every { node.numConnections } returns 2
            facade.onConnection(secondConnection)
            advanceUntilIdle()

            // Then both the list and the count follow — this arm must not be gated on subscribers
            assertEquals(2, facade.connectedPeers.value.size)
            assertEquals(2, facade.numConnections.value)
        }

    @Test
    fun `when a snapshot throws then the collector survives and the next tick refreshes`() =
        runTest {
            // Given an activated facade holding one peer
            activateFacade()
            assertEquals(1, facade.connectedPeers.value.size)

            // When a second peer connects but reading a peer's metrics blows up mid-snapshot (#1796: on the
            // Node 0.10.0 jar, before bisq2 fix 6707f25, ConnectionMetrics iterated a TreeMap its network
            // threads were mutating)
            every { node.allActiveConnections } answers { Stream.of(connection, secondConnection) }
            every { node.numConnections } returns 2
            every { metrics.sentBytes } throws ConcurrentModificationException()
            facade.onConnection(secondConnection)
            advanceUntilIdle()

            // Then the count (written before the mapping) stays live while the list keeps its previous
            // snapshot rather than a half-written one
            assertEquals(2, facade.numConnections.value)
            assertEquals(1, facade.connectedPeers.value.size)

            // And once the metrics read again, the very next tick refreshes — the single collector is alive
            every { metrics.sentBytes } returns 100L
            facade.onConnection(secondConnection)
            advanceUntilIdle()
            assertEquals(2, facade.connectedPeers.value.size)
            assertEquals(2, facade.numConnections.value)
        }

    @Test
    fun `when deactivated then the peer list is cleared and later ticks cannot repopulate it`() =
        runTest {
            // Given an activated facade
            activateFacade()
            assertEquals(1, facade.connectedPeers.value.size)

            // When deactivated
            facade.deactivate()
            advanceUntilIdle()

            // Then the collector is done before the flows are reset — no stale write survives
            assertTrue(facade.connectedPeers.value.isEmpty())

            // And a late listener callback cannot bring the list back
            facade.onConnection(connection)
            advanceTimeBy(pastRefreshInterval)
            advanceUntilIdle()
            assertTrue(facade.connectedPeers.value.isEmpty())
        }

    private suspend fun TestScope.activateFacade() {
        facade.activate()
        // The facade wires itself up when the ServiceNode reports INITIALIZING.
        serviceNodeState.set(ServiceNode.State.INITIALIZING)
        advanceUntilIdle()
    }

    private fun connectionMetrics(sentBytes: Long): ConnectionMetrics =
        mockk<ConnectionMetrics>(relaxed = true).also {
            every { it.averageRtt } returns 0.0
            every { it.sentBytes } returns sentBytes
            every { it.numMessagesSent } returns 1L
            every { it.receivedBytes } returns 200L
            every { it.numMessagesReceived } returns 2L
        }

    private fun connection(
        id: String,
        metrics: ConnectionMetrics,
    ): Connection =
        mockk<Connection>(relaxed = true).also {
            every { it.id } returns id
            every { it.peerAddress.fullAddress } returns "$id.onion:1234"
            every { it.isOutboundConnection } returns true
            every { it.created } returns 100L
            every { it.connectionMetrics } returns metrics
        }
}
