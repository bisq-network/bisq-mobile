package network.bisq.mobile.presentation.offer.create_offer.direction

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.data.replicated.common.currency.MarketVOFactory
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.data.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.data.service.offers.OffersServiceFacade
import network.bisq.mobile.data.service.reputation.ReputationServiceFacade
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.CurrencyUtils
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.test_utils.MainPresenterTestFactory
import network.bisq.mobile.presentation.common.test_utils.TestApplicationLifecycleService
import network.bisq.mobile.presentation.common.test_utils.coroutines.PlatformPresentationKoinTestBase
import network.bisq.mobile.presentation.common.test_utils.probeStateFlow
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.offer.create_offer.CreateOfferCoordinator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CreateOfferDirectionPresenterTest : PlatformPresentationKoinTestBase() {
    private lateinit var mainPresenter: MainPresenter
    private lateinit var userProfileServiceFacade: UserProfileServiceFacade
    private lateinit var reputationServiceFacade: ReputationServiceFacade

    override fun onKoinReady() {
        I18nSupport.initialize("en")
        mainPresenter =
            MainPresenterTestFactory.create(
                applicationLifecycleService = TestApplicationLifecycleService(),
            )
        userProfileServiceFacade = mockk(relaxed = true)
        reputationServiceFacade = mockk(relaxed = true)
    }

    @Test
    fun `when no market set then headline uses no-market copy`() =
        runTest {
            val presenter = createPresenter(market = null)
            val headlineProbe = probeStateFlow(presenter.headline)
            val marketNameProbe = probeStateFlow(presenter.marketName)
            advanceUntilIdle()

            assertEquals(
                "mobile.bisqEasy.tradeWizard.directionAndMarket.headlineNoMarket".i18n(),
                headlineProbe.latest(),
            )
            assertEquals(null, marketNameProbe.latest())
        }

    @Test
    fun `when market set then headline includes localized market name`() =
        runTest {
            val market = MarketVOFactory.USD
            val presenter = createPresenter(market = market)
            val headlineProbe = probeStateFlow(presenter.headline)
            val marketNameProbe = probeStateFlow(presenter.marketName)
            advanceUntilIdle()

            val localizedMarketName =
                CurrencyUtils.getLocaleFiatCurrencyName(
                    market.quoteCurrencyCode,
                    market.quoteCurrencyName,
                )
            assertEquals(localizedMarketName, marketNameProbe.latest())
            assertEquals(
                "mobile.bisqEasy.tradeWizard.directionAndMarket.headlineWithMarket".i18n(localizedMarketName),
                headlineProbe.latest(),
            )
        }

    @Test
    fun `when buy selected then commits buy and navigates to market step`() =
        runTest {
            val coordinator = makeCoordinator()
            val presenter = createPresenter(coordinator = coordinator)
            advanceUntilIdle()

            presenter.onBuySelected()
            advanceUntilIdle()

            assertEquals(DirectionEnum.BUY, coordinator.createOfferModel.direction)
            verify { navigationManager.navigate(NavRoute.CreateOfferMarket, any(), any()) }
        }

    @Test
    fun `when buy selected and currency skipped then navigates to amount step`() =
        runTest {
            val coordinator = makeCoordinator(skipCurrency = true)
            val presenter = createPresenter(coordinator = coordinator)
            advanceUntilIdle()

            presenter.onBuySelected()
            advanceUntilIdle()

            assertEquals(DirectionEnum.BUY, coordinator.createOfferModel.direction)
            verify { navigationManager.navigate(NavRoute.CreateOfferAmount, any(), any()) }
        }

    @Test
    fun `when sell selected with zero reputation then shows warning and does not navigate`() =
        runTest {
            val userProfile = createMockUserProfile("seller-profile")
            every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow(userProfile)
            coEvery { reputationServiceFacade.getReputation(userProfile.id) } returns
                Result.success(ReputationScoreVO(totalScore = 0L, fiveSystemScore = 0.0, ranking = 0))

            val coordinator = makeCoordinator()
            val presenter = createPresenter(coordinator = coordinator)
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onSellSelected()
            advanceUntilIdle()

            assertTrue(presenter.showSellerReputationWarning.value)
            assertEquals(DirectionEnum.BUY, coordinator.createOfferModel.direction)
            verify(exactly = 0) { navigationManager.navigate(NavRoute.CreateOfferMarket, any(), any()) }
            verify(exactly = 0) { navigationManager.navigate(NavRoute.CreateOfferAmount, any(), any()) }
        }

    @Test
    fun `when sell selected with reputation then commits sell and navigates`() =
        runTest {
            val userProfile = createMockUserProfile("seller-profile")
            every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow(userProfile)
            coEvery { reputationServiceFacade.getReputation(userProfile.id) } returns
                Result.success(ReputationScoreVO(totalScore = 30_000L, fiveSystemScore = 5.0, ranking = 1))

            val coordinator = makeCoordinator()
            val presenter = createPresenter(coordinator = coordinator)
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onSellSelected()
            advanceUntilIdle()

            assertFalse(presenter.showSellerReputationWarning.value)
            assertEquals(DirectionEnum.SELL, coordinator.createOfferModel.direction)
            verify { navigationManager.navigate(NavRoute.CreateOfferMarket, any(), any()) }
        }

    @Test
    fun `when close clicked then commits direction and navigates to offerbook tab`() =
        runTest {
            val coordinator = makeCoordinator(initialDirection = DirectionEnum.SELL)
            val presenter = createPresenter(coordinator = coordinator)
            advanceUntilIdle()

            presenter.onClose()
            advanceUntilIdle()

            assertEquals(DirectionEnum.SELL, coordinator.createOfferModel.direction)
            verify { navigationManager.navigateBackTo(NavRoute.TabContainer, any(), any()) }
            verify { navigationManager.navigateToTab(NavRoute.TabOfferbookMarket, any(), any(), any()) }
        }

    @Test
    fun `when navigate to reputation then navigates and dismisses warning`() =
        runTest {
            val presenter = createPresenter()
            presenter.setShowSellerReputationWarning(true)
            advanceUntilIdle()

            presenter.onNavigateToReputation()
            advanceUntilIdle()

            assertFalse(presenter.showSellerReputationWarning.value)
            verify { navigationManager.navigate(NavRoute.Reputation, any(), any()) }
        }

    @Test
    fun `when dismiss seller reputation warning then hides warning`() =
        runTest {
            val presenter = createPresenter()
            presenter.setShowSellerReputationWarning(true)
            advanceUntilIdle()

            presenter.onDismissSellerReputationWarning()
            advanceUntilIdle()

            assertFalse(presenter.showSellerReputationWarning.value)
        }

    private fun makeCoordinator(
        market: network.bisq.mobile.data.replicated.common.currency.MarketVO? = null,
        initialDirection: DirectionEnum = DirectionEnum.BUY,
        skipCurrency: Boolean = false,
    ): CreateOfferCoordinator {
        val coordinator =
            CreateOfferCoordinator(
                mockk<MarketPriceServiceFacade>(relaxed = true),
                mockk<OffersServiceFacade>(relaxed = true),
                mockk<SettingsServiceFacade>(relaxed = true),
            )
        coordinator.createOfferModel =
            CreateOfferCoordinator.CreateOfferModel().also { model ->
                model.market = market
                model.direction = initialDirection
            }
        coordinator.skipCurrency = skipCurrency
        return coordinator
    }

    private fun createPresenter(
        coordinator: CreateOfferCoordinator = makeCoordinator(),
        market: network.bisq.mobile.data.replicated.common.currency.MarketVO? = null,
    ): CreateOfferDirectionPresenter {
        if (market != null) {
            coordinator.createOfferModel.market = market
        }
        return CreateOfferDirectionPresenter(
            mainPresenter = mainPresenter,
            createOfferCoordinator = coordinator,
            userProfileServiceFacade = userProfileServiceFacade,
            reputationServiceFacade = reputationServiceFacade,
        )
    }
}
