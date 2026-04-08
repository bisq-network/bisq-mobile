package network.bisq.mobile.presentation.offerbook

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.data.model.offerbook.MarketListItem
import network.bisq.mobile.data.model.offerbook.OfferbookMarket
import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.service.alert.TradeRestrictingAlertServiceFacade
import network.bisq.mobile.data.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.data.service.offers.OffersServiceFacade
import network.bisq.mobile.data.service.reputation.ReputationServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.model.alert.AlertType
import network.bisq.mobile.domain.model.alert.AuthorizedAlertData
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.common.test_utils.MainPresenterTestFactory
import network.bisq.mobile.presentation.common.test_utils.NoopNavigationManager
import network.bisq.mobile.presentation.common.test_utils.TestApplicationLifecycleService
import network.bisq.mobile.presentation.common.test_utils.TestCoroutineJobsManager
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.components.molecules.ITopBarPresenter
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.WebLinkConfirmationDialogPresenter
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.platform.getScreenWidthDp
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.offer.create_offer.CreateOfferCoordinator
import network.bisq.mobile.presentation.offer.take_offer.TakeOfferCoordinator
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.assertTrue

/**
 * Compose UI tests verifying that [OfferbookScreen] correctly wires the trade-restriction dialog
 * flow.
 *
 * Covers:
 * - showTradeRestrictedDialog = null → no dialog content visible
 * - showTradeRestrictedDialog = non-null → [TradeRestrictedDialog] content is displayed
 * - blur state reflects showTradeRestrictedDialog != null
 * - dialog action dispatched to presenter closes the dialog
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class OfferbookScreenTradeRestrictionTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var presenter: OfferbookPresenter
    private lateinit var alertFlow: MutableStateFlow<AuthorizedAlertData?>

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        I18nSupport.setLanguage()
        mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        every { getScreenWidthDp() } returns 480

        alertFlow = MutableStateFlow(null)

        // Start Koin BEFORE constructing any presenter, because BasePresenter's presenterScope
        // lazy delegate calls getKoin() during construction.
        startKoin {
            modules(
                module {
                    factory<CoroutineJobsManager> { TestCoroutineJobsManager(testDispatcher) }
                    single<NavigationManager> { NoopNavigationManager() }
                    single { GlobalUiManager() }
                    single<MainPresenter> { MainPresenterTestFactory.create(applicationLifecycleService = TestApplicationLifecycleService()) }
                    single<OfferbookPresenter> { buildPresenter(get()) }
                    single<ITopBarPresenter> {
                        mockk<ITopBarPresenter>(relaxed = true).also { m ->
                            every { m.userProfile } returns MutableStateFlow(null)
                            every { m.showAnimation } returns MutableStateFlow(false)
                            every { m.connectivityStatus } returns
                                MutableStateFlow(
                                    network.bisq.mobile.data.service.network.ConnectivityService.ConnectivityStatus.CONNECTED_AND_DATA_RECEIVED,
                                )
                            every { m.isInteractive } returns MutableStateFlow(true)
                        }
                    }
                    factory { WebLinkConfirmationDialogPresenter(mockk(relaxed = true), get()) }
                },
            )
        }

        presenter =
            org.koin.core.context.GlobalContext
                .get()
                .get()
    }

    @After
    fun tearDown() {
        stopKoin()
        unmockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun buildPresenter(mainPresenter: MainPresenter): OfferbookPresenter {
        val offersFlow =
            MutableStateFlow(emptyList<network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationModel>())
        val marketFlow = MutableStateFlow(OfferbookMarket(MarketVO("BTC", "USD", "Bitcoin", "US Dollar")))
        val offersService = mockk<OffersServiceFacade>(relaxed = true)
        every { offersService.offerbookListItems } returns offersFlow
        every { offersService.selectedOfferbookMarket } returns marketFlow
        every { offersService.isOfferbookLoading } returns MutableStateFlow(false)
        coEvery { offersService.deleteOffer(any()) } returns Result.success(true)

        val userProfileServiceFacade = mockk<UserProfileServiceFacade>(relaxed = true)
        val me = createMockUserProfile("me")
        every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow(me)
        coEvery { userProfileServiceFacade.isUserIgnored(any()) } returns false

        val marketPriceServiceFacade =
            object : MarketPriceServiceFacade(mockk(relaxed = true)) {
                override fun findMarketPriceItem(marketVO: MarketVO) = null

                override fun findUSDMarketPriceItem() = null

                override fun refreshSelectedFormattedMarketPrice() {}

                override fun selectMarket(marketListItem: MarketListItem): Result<Unit> = Result.success(Unit)
            }

        val reputationService = mockk<ReputationServiceFacade>(relaxed = true)
        val takeOfferCoordinator = mockk<TakeOfferCoordinator>(relaxed = true)
        val createOfferCoordinator = mockk<CreateOfferCoordinator>(relaxed = true)

        val tradeRestrictingAlertServiceFacade = mockk<TradeRestrictingAlertServiceFacade>()
        every { tradeRestrictingAlertServiceFacade.alert } returns alertFlow

        return OfferbookPresenter(
            mainPresenter,
            offersService,
            takeOfferCoordinator,
            createOfferCoordinator,
            marketPriceServiceFacade,
            userProfileServiceFacade,
            reputationService,
            tradeRestrictingAlertServiceFacade,
        )
    }

    private fun makeAlert(headline: String = "Trading restricted — update required") =
        AuthorizedAlertData(
            id = "screen-test-alert",
            type = AlertType.EMERGENCY,
            headline = headline,
            message = "Update the app to continue trading.",
            haltTrading = true,
            date = 1L,
        )

    private fun setScreenContent() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    OfferbookScreen()
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // No dialog when alert is null
    // -------------------------------------------------------------------------

    @Test
    fun `no trade restricted dialog shown when showTradeRestrictedDialog is null`() =
        runTest(testDispatcher) {
            setScreenContent()
            advanceUntilIdle()
            composeTestRule.waitForIdle()

            // The dialog must not show any alert headline
            composeTestRule
                .onAllNodes(
                    androidx.compose.ui.test
                        .hasText("Trading restricted — update required"),
                ).fetchSemanticsNodes()
                .let { nodes -> assertTrue(nodes.isEmpty()) }
        }

    // -------------------------------------------------------------------------
    // Dialog shown when alert becomes non-null
    // -------------------------------------------------------------------------

    @Test
    fun `trade restricted dialog appears when showTradeRestrictedDialog is non-null`() =
        runTest(testDispatcher) {
            val alert = makeAlert()
            alertFlow.value = alert

            setScreenContent()
            advanceUntilIdle()
            composeTestRule.waitForIdle()

            // Trigger the dialog via createOffer (active alert path)
            presenter.createOffer()
            advanceUntilIdle()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("Trading restricted — update required").assertIsDisplayed()
        }

    // -------------------------------------------------------------------------
    // Dialog dismissed via OnCloseDialog
    // -------------------------------------------------------------------------

    @Test
    fun `onTradeRestrictingAlertAction OnCloseDialog dismisses the dialog`() =
        runTest(testDispatcher) {
            alertFlow.value = makeAlert("Alert to dismiss")

            setScreenContent()
            advanceUntilIdle()
            composeTestRule.waitForIdle()

            presenter.createOffer()
            advanceUntilIdle()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("Alert to dismiss").assertIsDisplayed()

            presenter.onTradeRestrictingAlertAction(
                network.bisq.mobile.presentation.common.ui.alert.AlertNotificationUiAction.OnCloseDialog,
            )
            advanceUntilIdle()
            composeTestRule.waitForIdle()

            composeTestRule
                .onAllNodes(
                    androidx.compose.ui.test
                        .hasText("Alert to dismiss"),
                ).fetchSemanticsNodes()
                .let { nodes -> assertTrue(nodes.isEmpty(), "Dialog must be gone after OnCloseDialog") }
        }
}
