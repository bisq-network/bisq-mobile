package network.bisq.mobile.node.network.presentation.my_node

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
import network.bisq.mobile.android.node.BuildNodeConfig
import network.bisq.mobile.data.service.network.KmpTorService
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.DefaultCoroutineJobsManager
import network.bisq.mobile.node.common.domain.service.network.NodeInfo
import network.bisq.mobile.node.common.domain.service.network.NodeNetworkServiceFacade
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
class NetworkMyNodePresenterTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var networkServiceFacade: NodeNetworkServiceFacade
    private lateinit var kmpTorService: KmpTorService
    private lateinit var mainPresenter: MainPresenter
    private lateinit var globalUiManager: GlobalUiManager
    private lateinit var navigationManager: NavigationManager

    private lateinit var myNodeInfo: MutableStateFlow<NodeInfo>
    private lateinit var torState: MutableStateFlow<KmpTorService.TorState>

    private lateinit var presenter: NetworkMyNodePresenter

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        networkServiceFacade = mockk(relaxed = true)
        kmpTorService = mockk(relaxed = true)
        mainPresenter = mockk(relaxed = true)
        globalUiManager = mockk(relaxed = true)
        navigationManager = mockk(relaxed = true)

        myNodeInfo = MutableStateFlow(NodeInfo())
        torState = MutableStateFlow(KmpTorService.TorState.Stopped())

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

    private fun createPresenter(): NetworkMyNodePresenter =
        NetworkMyNodePresenter(
            networkServiceFacade = networkServiceFacade,
            kmpTorService = kmpTorService,
            mainPresenter = mainPresenter,
        )

    @Test
    fun `when node info is resolved then uiState exposes address and keyId`() =
        runTest(testDispatcher) {
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
        runTest(testDispatcher) {
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
        runTest(testDispatcher) {
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
        runTest(testDispatcher) {
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
