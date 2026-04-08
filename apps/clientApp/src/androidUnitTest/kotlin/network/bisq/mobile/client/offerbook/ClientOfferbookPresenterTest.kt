package network.bisq.mobile.client.offerbook

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
import network.bisq.mobile.client.common.di.clientTestModule
import network.bisq.mobile.client.common.domain.service.user_profile.ClientUserProfileServiceFacade
import network.bisq.mobile.data.model.offerbook.MarketListItem
import network.bisq.mobile.data.model.offerbook.OfferbookMarket
import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.service.alert.TradeRestrictingAlertServiceFacade
import network.bisq.mobile.data.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.data.service.bootstrap.ApplicationLifecycleService
import network.bisq.mobile.data.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.data.service.network.KmpTorService
import network.bisq.mobile.data.service.offers.OffersServiceFacade
import network.bisq.mobile.data.service.reputation.ReputationServiceFacade
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.data.utils.UrlLauncher
import network.bisq.mobile.domain.model.alert.AlertType
import network.bisq.mobile.domain.model.alert.AuthorizedAlertData
import network.bisq.mobile.domain.repository.TradeReadStateRepository
import network.bisq.mobile.presentation.common.ui.platform.getScreenWidthDp
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.offer.create_offer.CreateOfferCoordinator
import network.bisq.mobile.presentation.offer.take_offer.TakeOfferCoordinator
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Verifies that [ClientOfferbookPresenter] correctly forwards the
 * [TradeRestrictingAlertServiceFacade] dependency to its [OfferbookPresenter] superclass.
 *
 * Tests exercise [createOffer] and [onOfferSelected] paths when a trade-restricting alert is
 * active, asserting that [showTradeRestrictedDialog] is populated from the facade — confirming
 * the forwarding logic in the constructor.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ClientOfferbookPresenterTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        every { getScreenWidthDp() } returns 480
        startKoin { modules(clientTestModule) }
    }

    @AfterTest
    fun tearDown() {
        unmockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        Dispatchers.resetMain()
        stopKoin()
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun buildMainPresenter(): MainPresenter {
        val tradesServiceFacade = mockk<TradesServiceFacade>(relaxed = true)
        every { tradesServiceFacade.openTradeItems } returns
            MutableStateFlow<List<TradeItemPresentationModel>>(
                emptyList(),
            )
        every { tradesServiceFacade.selectedTrade } returns MutableStateFlow<TradeItemPresentationModel?>(null)
        val userProfileServiceFacade =
            mockk<network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade>(relaxed = true)
        every { userProfileServiceFacade.ignoredProfileIds } returns MutableStateFlow<Set<String>>(emptySet())
        val settingsServiceFacade = mockk<SettingsServiceFacade>(relaxed = true)
        every { settingsServiceFacade.languageCode } returns MutableStateFlow("en")
        every { settingsServiceFacade.useAnimations } returns MutableStateFlow(false)
        val applicationLifecycleService =
            object : ApplicationLifecycleService(
                mockk<ApplicationBootstrapFacade>(relaxed = true),
                mockk<KmpTorService>(relaxed = true),
            ) {
                override suspend fun activateServiceFacades() {}

                override suspend fun deactivateServiceFacades() {}
            }
        return MainPresenter(
            tradesServiceFacade = tradesServiceFacade,
            userProfileServiceFacade = userProfileServiceFacade,
            openTradesNotificationService = mockk(relaxed = true),
            settingsService = settingsServiceFacade,
            tradeReadStateRepository = mockk<TradeReadStateRepository>(relaxed = true),
            urlLauncher = mockk<UrlLauncher>(relaxed = true),
            applicationLifecycleService = applicationLifecycleService,
        )
    }

    private fun buildPresenter(activeAlert: AuthorizedAlertData? = null): ClientOfferbookPresenter {
        val mainPresenter = buildMainPresenter()

        val offersFlow =
            MutableStateFlow(emptyList<network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationModel>())
        val marketFlow =
            MutableStateFlow(
                OfferbookMarket(MarketVO("BTC", "USD", "Bitcoin", "US Dollar")),
            )
        val offersService = mockk<OffersServiceFacade>(relaxed = true)
        every { offersService.offerbookListItems } returns offersFlow
        every { offersService.selectedOfferbookMarket } returns marketFlow
        every { offersService.isOfferbookLoading } returns MutableStateFlow(false)
        coEvery { offersService.deleteOffer(any()) } returns Result.success(true)

        val userProfileServiceFacade = mockk<ClientUserProfileServiceFacade>(relaxed = true)
        val me = createMockUserProfile("me")
        every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow(me)
        coEvery { userProfileServiceFacade.isUserIgnored(any()) } returns false
        every { userProfileServiceFacade.isUserIgnoredCached(any()) } returns false

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

        val alertFlow = MutableStateFlow<AuthorizedAlertData?>(activeAlert)
        val tradeRestrictingAlertServiceFacade = mockk<TradeRestrictingAlertServiceFacade>()
        every { tradeRestrictingAlertServiceFacade.alert } returns alertFlow

        return ClientOfferbookPresenter(
            mainPresenter = mainPresenter,
            offersServiceFacade = offersService,
            takeOfferCoordinator = takeOfferCoordinator,
            createOfferCoordinator = createOfferCoordinator,
            marketPriceServiceFacade = marketPriceServiceFacade,
            reputationServiceFacade = reputationService,
            userProfileServiceFacade = userProfileServiceFacade,
            tradeRestrictingAlertServiceFacade = tradeRestrictingAlertServiceFacade,
        )
    }

    private fun makeAlert(headline: String = "Trading restricted") =
        AuthorizedAlertData(
            id = "test-alert",
            type = AlertType.EMERGENCY,
            headline = headline,
            message = "Please update.",
            haltTrading = true,
            date = 1L,
        )

    // -------------------------------------------------------------------------
    // createOffer — trade-restricting alert path
    // -------------------------------------------------------------------------

    @Test
    fun `createOffer with active alert sets showTradeRestrictedDialog via forwarded facade`() =
        runTest(testDispatcher) {
            val alert = makeAlert()
            val presenter = buildPresenter(activeAlert = alert)
            assertNull(presenter.showTradeRestrictedDialog.value)

            presenter.createOffer()
            advanceUntilIdle()

            val dialogState = presenter.showTradeRestrictedDialog.value
            assertNotNull(dialogState)
            assertEquals("test-alert", dialogState.id)
            assertEquals("Trading restricted", dialogState.headline)
        }

    @Test
    fun `createOffer without active alert leaves showTradeRestrictedDialog null`() =
        runTest(testDispatcher) {
            val presenter = buildPresenter(activeAlert = null)
            presenter.onViewAttached()
            advanceUntilIdle()

            // No navigation happens because createOfferCoordinator is a relaxed mock,
            // but the dialog must stay null.
            presenter.createOffer()
            advanceUntilIdle()

            assertNull(presenter.showTradeRestrictedDialog.value)
        }

    // -------------------------------------------------------------------------
    // onTradeRestrictingAlertAction — forwarded to superclass
    // -------------------------------------------------------------------------

    @Test
    fun `OnCloseDialog action clears showTradeRestrictedDialog`() =
        runTest(testDispatcher) {
            val presenter = buildPresenter(activeAlert = makeAlert())
            presenter.createOffer()
            advanceUntilIdle()
            assertNotNull(presenter.showTradeRestrictedDialog.value)

            presenter.onTradeRestrictingAlertAction(
                network.bisq.mobile.presentation.common.ui.alert.AlertNotificationUiAction.OnCloseDialog,
            )
            advanceUntilIdle()

            assertNull(presenter.showTradeRestrictedDialog.value)
        }

    @Test
    fun `OnUpdateNow action clears showTradeRestrictedDialog`() =
        runTest(testDispatcher) {
            val presenter = buildPresenter(activeAlert = makeAlert())
            presenter.createOffer()
            advanceUntilIdle()
            assertNotNull(presenter.showTradeRestrictedDialog.value)

            presenter.onTradeRestrictingAlertAction(
                network.bisq.mobile.presentation.common.ui.alert.AlertNotificationUiAction.OnUpdateNow,
            )
            advanceUntilIdle()

            assertNull(presenter.showTradeRestrictedDialog.value)
        }
}
