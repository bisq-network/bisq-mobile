package network.bisq.mobile.presentation.tabs.more

import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.nav_settings
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.i18n.UiString
import network.bisq.mobile.presentation.common.test_utils.coroutines.PresentationKoinTestBase
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MiscItemsPresenterTest : PresentationKoinTestBase() {
    private lateinit var userProfileService: UserProfileServiceFacade
    private lateinit var mainPresenter: MainPresenter

    private lateinit var presenter: MiscItemsPresenter

    override fun onKoinReady() {
        userProfileService = mockk(relaxed = true)
        mainPresenter = mockk(relaxed = true)
        coEvery { userProfileService.getIgnoredUserProfileIds() } returns emptySet()
    }

    private fun createPresenter(): MiscItemsPresenter = TestMiscItemsPresenter(userProfileService, mainPresenter)

    private fun ignoredUsersItem(): MenuItem =
        presenter.uiState.value.sections
            .flatMap { it.items }
            .first { it.route == NavRoute.IgnoredUsers }

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
    fun `when no ignored users then ignored users item is disabled`() =
        runTest {
            // Given
            coEvery { userProfileService.getIgnoredUserProfileIds() } returns emptySet()
            presenter = createPresenter()

            // When
            presenter.onViewAttached()
            advanceUntilIdle()

            // Then
            assertFalse(ignoredUsersItem().isEnabled)
        }

    @Test
    fun `when ignored users exist then ignored users item is enabled`() =
        runTest {
            // Given
            coEvery { userProfileService.getIgnoredUserProfileIds() } returns setOf("ignored-user-id")
            presenter = createPresenter()

            // When
            presenter.onViewAttached()
            advanceUntilIdle()

            // Then
            assertTrue(ignoredUsersItem().isEnabled)
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

    /**
     * Minimal concrete subclass providing the two abstract hooks so the shared behaviour can be exercised
     * without pulling in per-app resource/BuildConfig dependencies.
     */
    private class TestMiscItemsPresenter(
        userProfileService: UserProfileServiceFacade,
        mainPresenter: MainPresenter,
    ) : MiscItemsPresenter(userProfileService, mainPresenter) {
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
}
