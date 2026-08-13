package network.bisq.mobile.node.network.presentation.my_node

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.android.node.BuildNodeConfig
import network.bisq.mobile.data.service.network.KmpTorService
import network.bisq.mobile.node.common.domain.service.network.NodeInfo
import network.bisq.mobile.node.common.domain.service.network.NodeNetworkServiceFacade
import network.bisq.mobile.node.common.test_utils.NodeKoinIntegrationTestBase
import network.bisq.mobile.presentation.main.MainPresenter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkMyNodePresenterTest : NodeKoinIntegrationTestBase() {
    private val networkServiceFacade: NodeNetworkServiceFacade = mockk(relaxed = true)
    private val kmpTorService: KmpTorService = mockk(relaxed = true)
    private val mainPresenter: MainPresenter = mockk(relaxed = true)

    private val myNodeInfo = MutableStateFlow(NodeInfo())
    private val torState = MutableStateFlow<KmpTorService.TorState>(KmpTorService.TorState.Stopped())

    private lateinit var presenter: NetworkMyNodePresenter

    override fun onSetup() {
        every { networkServiceFacade.myNodeInfo } returns myNodeInfo
        every { kmpTorService.state } returns torState
    }

    private fun createPresenter(): NetworkMyNodePresenter =
        NetworkMyNodePresenter(
            networkServiceFacade = networkServiceFacade,
            kmpTorService = kmpTorService,
            mainPresenter = mainPresenter,
        )

    @Test
    fun `when node info is resolved then uiState exposes address and keyId`() =
        runTest {
            // Given
            myNodeInfo.value = NodeInfo(onionAddress = "abcd.onion:1234", keyId = "135e9801")
            torState.value = KmpTorService.TorState.Started

            // When
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals("abcd.onion:1234", state.onionAddress)
            assertEquals("135e9801", state.keyId)
            assertEquals(BuildNodeConfig.APP_VERSION, state.appVersion)
            assertTrue(state.isTorRunning)
        }

    @Test
    fun `when node info is not yet resolved then address and keyId are null`() =
        runTest {
            // Given no node info

            // When
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals(null, state.onionAddress)
            assertEquals(null, state.keyId)
        }

    @Test
    fun `when tor is not started then isTorRunning is false`() =
        runTest {
            // Given
            torState.value = KmpTorService.TorState.Stopped()

            // When
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // Then
            assertFalse(presenter.uiState.value.isTorRunning)
        }

    @Test
    fun `when node info arrives then uiState updates reactively`() =
        runTest {
            // Given an attached presenter with no node info
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()
            assertEquals(null, presenter.uiState.value.onionAddress)

            // When the address resolves
            myNodeInfo.value = NodeInfo(onionAddress = "resolved.onion:1234", keyId = "abc")
            advanceUntilIdle()

            // Then
            assertEquals("resolved.onion:1234", presenter.uiState.value.onionAddress)
            assertEquals("abc", presenter.uiState.value.keyId)
        }
}
