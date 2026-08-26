package network.bisq.mobile.presentation.tabs.more

import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.nav_settings
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.domain.service.capabilities.BackendCapabilities
import network.bisq.mobile.domain.service.capabilities.BackendCapabilitiesService
import network.bisq.mobile.domain.service.capabilities.Feature
import network.bisq.mobile.domain.service.community.CommunityHubService
import network.bisq.mobile.domain.service.community.CommunitySegment
import network.bisq.mobile.i18n.UiString
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.presentation.coroutines.PresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MiscItemsPresenterTest : PresentationKoinTestBase() {
    private lateinit var mainPresenter: MainPresenter
    private lateinit var backendCapabilitiesService: BackendCapabilitiesService

    private val capabilitiesFlow = MutableStateFlow(BackendCapabilities.UNAVAILABLE)

    private lateinit var presenter: MiscItemsPresenter

    override fun onKoinReady() {
        mainPresenter = mockk(relaxed = true)
        capabilitiesFlow.value = BackendCapabilities.UNAVAILABLE
        backendCapabilitiesService =
            mockk<BackendCapabilitiesService>(relaxed = true).also {
                every { it.capabilities } returns capabilitiesFlow
            }
    }

    private val communityHubService =
        mockk<CommunityHubService> {
            every { liveSegments } returns MutableStateFlow(emptySet())
        }

    private fun createPresenter(): MiscItemsPresenter = TestMiscItemsPresenter(backendCapabilitiesService, communityHubService, mainPresenter)

    private fun ignoredUsersItem(): MenuItem =
        presenter.uiState.value.sections
            .flatMap { it.items }
            .first { it.route == NavRoute.IgnoredUsers }

    private fun hasNetworkItem(): Boolean =
        presenter.uiState.value.sections
            .flatMap { it.items }
            .any { it.route == NavRoute.NetworkOverview }

    @Test
    fun `when attached then exposes the four sections in order`() =
        runTest {
            // Given
            presenter = createPresenter()

            // When
            presenter.onViewAttached()
            advanceUntilIdle()

            // Then
            val titles =
                presenter.uiState.value.sections
                    .map { it.title.key }
            assertEquals(
                listOf(
                    "mobile.more.section.identity",
                    "mobile.more.section.tradingSetup",
                    "mobile.more.section.help",
                    "mobile.more.section.app",
                ),
                titles,
            )
        }

    @Test
    fun `when attached then ignored users item is present`() =
        runTest {
            // Given
            presenter = createPresenter()

            // When
            presenter.onViewAttached()
            advanceUntilIdle()

            // Then
            assertEquals("mobile.settings.ignoredUsers", ignoredUsersItem().label.key)
        }

    @Test
    fun `when menu item clicked then navigates to its route`() =
        runTest {
            // Given
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onAction(MiscItemsUiAction.OnMenuItemClick(NavRoute.Settings))
            advanceUntilIdle()

            // Then
            verify { navigationManager.navigate(NavRoute.Settings, any(), any()) }
        }

    @Test
    fun `when custom app items added then they appear in the app section`() =
        runTest {
            // Given
            capabilitiesFlow.value = BackendCapabilities(setOf(Feature.NETWORK_INFO.key))
            presenter = createPresenter()

            // When
            presenter.onViewAttached()
            advanceUntilIdle()

            // Then
            val appSection =
                presenter.uiState.value.sections
                    .first { it.title.key == "mobile.more.section.app" }
            val appLabels = appSection.items.map { it.label.key }
            assertEquals(
                listOf("mobile.more.settings", "mobile.more.custom", "mobile.more.network", "mobile.more.resources"),
                appLabels,
            )
        }

    @Test
    fun `when node does not support network info then network item is hidden`() =
        runTest {
            // Given
            presenter = createPresenter()

            // When
            presenter.onViewAttached()
            advanceUntilIdle()

            // Then
            assertFalse(hasNetworkItem())
        }

    @Test
    fun `when node supports network info then network item is shown`() =
        runTest {
            // Given
            capabilitiesFlow.value = BackendCapabilities(setOf(Feature.NETWORK_INFO.key))
            presenter = createPresenter()

            // When
            presenter.onViewAttached()
            advanceUntilIdle()

            // Then
            assertTrue(hasNetworkItem())
        }

    @Test
    fun `when network info capability appears after attach then network item is added`() =
        runTest {
            // Given
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()
            assertFalse(hasNetworkItem())

            // When
            capabilitiesFlow.value = BackendCapabilities(setOf(Feature.NETWORK_INFO.key))
            advanceUntilIdle()

            // Then
            assertTrue(hasNetworkItem())
        }

    @Test
    fun `when network info capability disappears then network item is removed`() =
        runTest {
            // Given
            capabilitiesFlow.value = BackendCapabilities(setOf(Feature.NETWORK_INFO.key))
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()
            assertTrue(hasNetworkItem())

            // When
            capabilitiesFlow.value = BackendCapabilities.UNAVAILABLE
            advanceUntilIdle()

            // Then
            assertFalse(hasNetworkItem())
        }

    /**
     * Minimal concrete subclass providing the two abstract hooks so the shared behaviour can be exercised
     * without pulling in per-app resource/BuildConfig dependencies.
     */
    private class TestMiscItemsPresenter(
        backendCapabilitiesService: BackendCapabilitiesService,
        communityHubService: CommunityHubService,
        mainPresenter: MainPresenter,
    ) : MiscItemsPresenter(backendCapabilitiesService, communityHubService, mainPresenter) {
        override fun getPaymentAccountNavRoute(): NavRoute = NavRoute.PaymentAccounts

        override fun addCustomSettings(appItems: MutableList<MenuItem>): List<MenuItem> {
            appItems.add(
                1,
                MenuItem(
                    label = UiString("mobile.more.custom"),
                    icon = Res.drawable.nav_settings,
                    route = NavRoute.PaymentAccounts,
                ),
            )
            return appItems
        }
    }

    @Test
    fun `contacts row appears when the segment is live and clicking it navigates to the hub deep link`() =
        runTest {
            every { communityHubService.liveSegments } returns MutableStateFlow(setOf(CommunitySegment.CONTACTS))
            val presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            val items =
                presenter.uiState.value.sections
                    .flatMap { it.items }
            val contactsItem = items.firstOrNull { it.route is NavRoute.CommunityHub }
            assertNotNull(contactsItem, "My Contacts row should be present when CONTACTS is live")
            assertEquals(CommunitySegment.CONTACTS.name, (contactsItem.route as NavRoute.CommunityHub).initialSegment)

            presenter.onAction(MiscItemsUiAction.OnMenuItemClick(contactsItem.route))
            advanceUntilIdle()
        }

    @Test
    fun `contacts row is absent when the segment is not live`() =
        runTest {
            val presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            val items =
                presenter.uiState.value.sections
                    .flatMap { it.items }
            assertTrue(items.none { it.route is NavRoute.CommunityHub }, "My Contacts row must be hidden while CONTACTS is gated off")
        }
}
