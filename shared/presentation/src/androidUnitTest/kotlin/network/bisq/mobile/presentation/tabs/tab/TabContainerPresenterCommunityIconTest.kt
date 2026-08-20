package network.bisq.mobile.presentation.tabs.tab

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.data.service.alert.TradeRestrictingAlertServiceFacade
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.capabilities.BackendCapabilities
import network.bisq.mobile.domain.service.capabilities.BackendCapabilitiesService
import network.bisq.mobile.domain.service.capabilities.Feature
import network.bisq.mobile.domain.service.community.CommunityHubService
import network.bisq.mobile.domain.service.community.CommunitySegment
import network.bisq.mobile.presentation.common.test_utils.FakeAppUpdateLinker
import network.bisq.mobile.presentation.common.test_utils.MainPresenterTestFactory
import network.bisq.mobile.presentation.common.test_utils.TestApplicationLifecycleService
import network.bisq.mobile.presentation.common.ui.animation.AnimationSettings
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.offer.create_offer.CreateOfferCoordinator
import network.bisq.mobile.test.presentation.coroutines.PlatformPresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins the Community entry icon's visibility and badge feed: visible iff any hub segment is
 * live (reactively), badge count sourced from [CommunityHubService.unreadCount], tap
 * navigating to the hub route.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TabContainerPresenterCommunityIconTest : PlatformPresentationKoinTestBase() {
    private val createOfferCoordinator: CreateOfferCoordinator = mockk(relaxed = true)
    private val capabilitiesFlow = MutableStateFlow(BackendCapabilities.UNAVAILABLE)

    override fun beforeStartKoin() {
        super.beforeStartKoin()
        globalUiManager = GlobalUiManager(testDispatcher)
    }

    private fun kotlinx.coroutines.test.TestScope.buildPresenter(
        shipped: Set<CommunitySegment> = emptySet(),
        devForced: Set<CommunitySegment> = emptySet(),
        requiredFeatures: Map<CommunitySegment, Feature> = emptyMap(),
    ): Pair<TabContainerPresenter, CommunityHubService> {
        val settingsServiceFacade = mockk<SettingsServiceFacade>(relaxed = true)
        every { settingsServiceFacade.useAnimations } returns MutableStateFlow(true)
        val tradeRestrictingAlertServiceFacade = mockk<TradeRestrictingAlertServiceFacade>(relaxed = true)
        every { tradeRestrictingAlertServiceFacade.alert } returns MutableStateFlow(null)
        val communityHubService =
            CommunityHubService(
                backendCapabilitiesService =
                    object : BackendCapabilitiesService {
                        override val capabilities: StateFlow<BackendCapabilities> = capabilitiesFlow
                    },
                shippedSegments = shipped,
                devForcedSegments = devForced,
                requiredFeatures = requiredFeatures,
                dispatcher = UnconfinedTestDispatcher(testScheduler),
            )
        val mainPresenter =
            MainPresenterTestFactory.create(
                applicationLifecycleService = TestApplicationLifecycleService(),
            )
        val presenter =
            TabContainerPresenter(
                mainPresenter,
                createOfferCoordinator,
                settingsServiceFacade,
                tradeRestrictingAlertServiceFacade,
                FakeAppUpdateLinker(),
                AnimationSettings(settingsServiceFacade, mockk(relaxed = true), applyDeviceLock = false),
                communityHubService,
            )
        return presenter to communityHubService
    }

    @Test
    fun `icon is hidden while no segment is live`() =
        runTest {
            val (presenter, _) = buildPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            assertFalse(presenter.communityIconVisible.value)
        }

    @Test
    fun `icon is visible when a segment is live`() =
        runTest {
            val (presenter, _) = buildPresenter(devForced = setOf(CommunitySegment.DISCUSSIONS))
            presenter.onViewAttached()
            advanceUntilIdle()

            assertTrue(presenter.communityIconVisible.value)
        }

    @Test
    fun `icon appears when the backend capability arrives`() =
        runTest {
            val (presenter, _) =
                buildPresenter(
                    shipped = setOf(CommunitySegment.DISCUSSIONS),
                    requiredFeatures = mapOf(CommunitySegment.DISCUSSIONS to Feature.NETWORK_INFO),
                )
            presenter.onViewAttached()
            advanceUntilIdle()
            assertFalse(presenter.communityIconVisible.value)

            capabilitiesFlow.value = BackendCapabilities(setOf(Feature.NETWORK_INFO.key))
            advanceUntilIdle()

            assertTrue(presenter.communityIconVisible.value)
        }

    @Test
    fun `badge count follows the hub service unread count`() =
        runTest {
            val (presenter, service) = buildPresenter(devForced = setOf(CommunitySegment.DISCUSSIONS))
            presenter.onViewAttached()
            advanceUntilIdle()
            assertEquals(0, presenter.communityUnreadCount.value)

            service.setUnreadCount(7)
            advanceUntilIdle()

            assertEquals(7, presenter.communityUnreadCount.value)
        }

    @Test
    fun `open community hub navigates to the hub route`() =
        runTest {
            val (presenter, _) = buildPresenter(devForced = setOf(CommunitySegment.DISCUSSIONS))
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.openCommunityHub()
            advanceUntilIdle()

            verify { navigationManager.navigate(NavRoute.CommunityHub, any(), any()) }
        }
}
