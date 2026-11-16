package network.bisq.mobile.presentation.offer.create_offer.market

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.data.model.offerbook.MarketListItem
import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.replicated.common.currency.MarketVOFactory
import network.bisq.mobile.data.service.offers.OffersServiceFacade
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.common.test_utils.FakeMarketPriceServiceFacade
import network.bisq.mobile.presentation.common.test_utils.MainPresenterTestFactory
import network.bisq.mobile.presentation.common.test_utils.OfferTestFactory
import network.bisq.mobile.presentation.common.test_utils.TestApplicationLifecycleService
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.offer.create_offer.CreateOfferCoordinator
import network.bisq.mobile.test.mocks.SettingsRepositoryMock
import network.bisq.mobile.test.presentation.coroutines.PlatformPresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class CreateOfferMarketPresenterTest : PlatformPresentationKoinTestBase() {
    private val offersServiceFacade =
        mockk<OffersServiceFacade>(relaxed = true).also {
            every { it.offerbookMarketItems } returns MutableStateFlow(emptyList())
            every { it.selectOfferbookMarket(any()) } returns Result.success(Unit)
        }

    override fun onKoinReady() {
        I18nSupport.initialize("en")
    }

    private fun createPresenter(coordinator: CreateOfferCoordinator) =
        CreateOfferMarketPresenter(
            MainPresenterTestFactory.create(applicationLifecycleService = TestApplicationLifecycleService()),
            offersServiceFacade,
            coordinator,
            FakeMarketPriceServiceFacade(SettingsRepositoryMock()),
        )

    private fun coordinator() =
        CreateOfferCoordinator(
            FakeMarketPriceServiceFacade(SettingsRepositoryMock(), OfferTestFactory.usdPrices()),
            offersServiceFacade,
            mockk(relaxed = true),
        ).apply { onStartCreateOffer() }

    @Test
    fun `selecting a market without a price shows an error and stays on the market step`() =
        runTest {
            val coordinator = coordinator()
            val presenter = createPresenter(coordinator)

            presenter.onSelectMarket(MarketListItem.from(MarketVO("BTC", "COP")))
            advanceUntilIdle()

            assertNull(coordinator.createOfferModel.market)
            verify(exactly = 0) { navigationManager.navigate(NavRoute.CreateOfferAmount, any(), any()) }
            verify { globalUiManager.showSnackbar(any(), any(), any(), any()) }
        }

    @Test
    fun `selecting a market with a price commits it and opens the amount step`() =
        runTest {
            val coordinator = coordinator()
            val presenter = createPresenter(coordinator)
            val usdItem = MarketListItem.from(MarketVOFactory.USD)

            presenter.onSelectMarket(usdItem)
            advanceUntilIdle()

            assertEquals(MarketVOFactory.USD, coordinator.createOfferModel.market)
            verify { offersServiceFacade.selectOfferbookMarket(usdItem) }
            verify { navigationManager.navigate(NavRoute.CreateOfferAmount, any(), any()) }
        }

    @Test
    fun `closing commits the selected market and returns to the offerbook`() =
        runTest {
            val coordinator = coordinator()
            val presenter = createPresenter(coordinator)
            presenter.onSelectMarket(MarketListItem.from(MarketVO("BTC", "COP")))

            presenter.onClose()
            advanceUntilIdle()

            assertNull(coordinator.createOfferModel.market)
            verify { navigationManager.navigateToTab(NavRoute.TabOfferbookMarket, any(), any(), any()) }
        }

    @Test
    fun `going back commits the selected market`() =
        runTest {
            val coordinator = coordinator()
            val presenter = createPresenter(coordinator)
            presenter.onSelectMarket(MarketListItem.from(MarketVOFactory.USD))

            presenter.onBack()
            advanceUntilIdle()

            assertEquals(MarketVOFactory.USD, coordinator.createOfferModel.market)
            verify { navigationManager.navigateBack(any()) }
        }
}
