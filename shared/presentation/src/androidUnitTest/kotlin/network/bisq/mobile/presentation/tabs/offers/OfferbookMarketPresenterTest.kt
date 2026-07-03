package network.bisq.mobile.presentation.tabs.offers

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.data.model.market.MarketFilter
import network.bisq.mobile.data.model.market.MarketPriceItem
import network.bisq.mobile.data.model.market.MarketSortBy
import network.bisq.mobile.data.model.offerbook.MarketListItem
import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOFactory
import network.bisq.mobile.data.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.data.service.offers.OffersServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.repository.SettingsRepository
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.tabs.offers.usecase.ComputeOfferbookMarketListUseCase
import network.bisq.mobile.test.coroutines.StandardTestDispatcherProvider
import network.bisq.mobile.test.coroutines.TestCoroutineJobsManager
import network.bisq.mobile.test.mocks.SettingsRepositoryMock
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Behavior of the migrated (UiState/UiAction) [OfferbookMarketPresenter]:
 * settings persistence, derived [OfferbookMarketUiState], search filtering, back-stack
 * hide/reveal survival, and market-selection navigation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OfferbookMarketPresenterTest {
    private val dispatcherProvider = StandardTestDispatcherProvider()
    private val testDispatcher = dispatcherProvider.default
    private lateinit var navigationManager: NavigationManager
    private lateinit var globalUiManager: GlobalUiManager

    private val offersServiceFacade: OffersServiceFacade =
        mockk<OffersServiceFacade>(relaxed = true).also {
            every { it.offerbookMarketItems } returns MutableStateFlow(emptyList())
        }

    private val userProfileServiceFacade: UserProfileServiceFacade =
        mockk<UserProfileServiceFacade>(relaxed = true).also {
            every { it.ignoredProfileIds } returns MutableStateFlow(emptySet())
        }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        navigationManager = mockk(relaxed = true)
        globalUiManager = mockk(relaxed = true)

        startKoin {
            modules(
                module {
                    factory<CoroutineJobsManager> { TestCoroutineJobsManager(testDispatcher) }
                    single<NavigationManager> { navigationManager }
                    single { globalUiManager }
                },
            )
        }
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    @Test
    fun `when filter changed then it is persisted and reflected in uiState`() =
        runTest(testDispatcher) {
            // Given
            val settingsRepository = SettingsRepositoryMock()
            val presenter = createPresenter(settingsRepository)
            advanceUntilIdle()

            // When
            presenter.onAction(OfferbookMarketUiAction.OnFilterChanged(MarketFilter.WithOffers))
            advanceUntilIdle()

            // Then
            assertEquals(MarketFilter.WithOffers, settingsRepository.data.value.marketFilter)
            assertEquals(MarketFilter.WithOffers, presenter.uiState.value.filter)
        }

    @Test
    fun `when sort by changed then it is persisted and reflected in uiState`() =
        runTest(testDispatcher) {
            // Given
            val settingsRepository = SettingsRepositoryMock()
            val presenter = createPresenter(settingsRepository)
            advanceUntilIdle()

            // When
            presenter.onAction(OfferbookMarketUiAction.OnSortByChanged(MarketSortBy.NameAZ))
            advanceUntilIdle()

            // Then
            assertEquals(MarketSortBy.NameAZ, settingsRepository.data.value.marketSortBy)
            assertEquals(MarketSortBy.NameAZ, presenter.uiState.value.sortBy)
        }

    @Test
    fun `when search text changed then searchText updates and market list is filtered`() =
        runTest(testDispatcher) {
            // Given
            val presenter = presenterWithMarkets()
            advanceUntilIdle()
            assertEquals(listOf("USD", "EUR", "BRL"), presenter.marketCodes())

            // When
            presenter.onAction(OfferbookMarketUiAction.OnSearchTextChanged("eu"))
            advanceUntilIdle()

            // Then
            assertEquals("eu", presenter.searchText.value)
            assertEquals(listOf("EUR"), presenter.marketCodes())
        }

    @Test
    fun `when hidden and revealed then market filtering still works`() =
        runTest(testDispatcher) {
            // Given
            val presenter = presenterWithMarkets()
            advanceUntilIdle()

            // When — back-stack-aware lifecycle hides/reveals without disposing presenterScope
            presenter.onViewHidden()
            advanceUntilIdle()
            presenter.onViewRevealed()
            advanceUntilIdle()

            presenter.onAction(OfferbookMarketUiAction.OnSearchTextChanged("br"))
            advanceUntilIdle()

            // Then
            assertEquals(listOf("BRL"), presenter.marketCodes())
        }

    @Test
    fun `when there are ignored users then uiState hasIgnoredUsers is true`() =
        runTest(testDispatcher) {
            // Given
            val userProfileServiceFacade =
                mockk<UserProfileServiceFacade>(relaxed = true).also {
                    every { it.ignoredProfileIds } returns MutableStateFlow(setOf("ignored-id"))
                }
            val presenter =
                createPresenter(
                    settingsRepository = SettingsRepositoryMock(),
                    userProfileServiceFacade = userProfileServiceFacade,
                )

            // When
            advanceUntilIdle()

            // Then
            assertTrue(presenter.uiState.value.hasIgnoredUsers)
        }

    @Test
    fun `when market selected successfully then navigates to offerbook`() =
        runTest(testDispatcher) {
            // Given
            val item = marketItem("USD", "US Dollar", numOffers = 1)
            val offersServiceFacade =
                mockk<OffersServiceFacade>(relaxed = true).also {
                    every { it.offerbookMarketItems } returns MutableStateFlow(emptyList())
                    every { it.selectOfferbookMarket(item) } returns Result.success(Unit)
                }
            val presenter =
                createPresenter(
                    settingsRepository = SettingsRepositoryMock(),
                    offersServiceFacade = offersServiceFacade,
                )

            // When
            presenter.onAction(OfferbookMarketUiAction.OnMarketSelected(item))
            advanceUntilIdle()

            // Then
            verify { navigationManager.navigate(NavRoute.Offerbook, any(), any()) }
        }

    @Test
    fun `when market selection fails then does not navigate and shows error snackbar`() =
        runTest(testDispatcher) {
            // Given
            val item = marketItem("USD", "US Dollar", numOffers = 1)
            val offersServiceFacade =
                mockk<OffersServiceFacade>(relaxed = true).also {
                    every { it.offerbookMarketItems } returns MutableStateFlow(emptyList())
                    every { it.selectOfferbookMarket(item) } returns Result.failure(RuntimeException("boom"))
                }
            val presenter =
                createPresenter(
                    settingsRepository = SettingsRepositoryMock(),
                    offersServiceFacade = offersServiceFacade,
                )

            // When
            presenter.onAction(OfferbookMarketUiAction.OnMarketSelected(item))
            advanceUntilIdle()

            // Then
            verify(exactly = 0) { navigationManager.navigate(NavRoute.Offerbook, any(), any()) }
            verify { globalUiManager.showSnackbar(any(), any(), any(), any()) }
        }

    @Test
    fun `initial uiState exposes default filter and sort with an empty list`() =
        runTest(testDispatcher) {
            // Given / When
            val presenter = createPresenter(SettingsRepositoryMock())
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals(MarketFilter.All, state.filter)
            assertEquals(MarketSortBy.MostOffers, state.sortBy)
            assertTrue(state.marketItems.isEmpty())
            assertFalse(state.hasIgnoredUsers)
        }

    private fun createPresenter(
        settingsRepository: SettingsRepository,
        offersServiceFacade: OffersServiceFacade = this.offersServiceFacade,
        marketPriceServiceFacade: MarketPriceServiceFacade =
            FakeMarketPriceServiceFacade(settingsRepository, marketsWithPrice = emptySet()),
        userProfileServiceFacade: UserProfileServiceFacade = this.userProfileServiceFacade,
    ): OfferbookMarketPresenter {
        val mainPresenter =
            mockk<MainPresenter>(relaxed = true) {
                every { languageCode } returns MutableStateFlow("en")
            }
        return OfferbookMarketPresenter(
            mainPresenter = mainPresenter,
            offersServiceFacade = offersServiceFacade,
            marketPriceServiceFacade = marketPriceServiceFacade,
            userProfileServiceFacade = userProfileServiceFacade,
            settingsRepository = settingsRepository,
            computeOfferbookMarketListUseCase = ComputeOfferbookMarketListUseCase(marketPriceServiceFacade),
            dispatcherProvider = dispatcherProvider,
        )
    }

    private fun presenterWithMarkets(): OfferbookMarketPresenter {
        val settingsRepository = SettingsRepositoryMock()
        val offersServiceFacade =
            mockk<OffersServiceFacade>(relaxed = true).also {
                every { it.offerbookMarketItems } returns
                    MutableStateFlow(
                        listOf(
                            marketItem("USD", "US Dollar", numOffers = 3),
                            marketItem("EUR", "Euro", numOffers = 2),
                            marketItem("BRL", "Brazilian Real", numOffers = 1),
                        ),
                    )
            }
        val marketPriceServiceFacade =
            FakeMarketPriceServiceFacade(settingsRepository, marketsWithPrice = setOf("USD", "EUR", "BRL"))
        return createPresenter(
            settingsRepository = settingsRepository,
            offersServiceFacade = offersServiceFacade,
            marketPriceServiceFacade = marketPriceServiceFacade,
        )
    }

    private fun OfferbookMarketPresenter.marketCodes(): List<String> = uiState.value.marketItems.map { it.market.quoteCurrencyCode }

    private fun marketItem(
        quoteCode: String,
        quoteName: String,
        numOffers: Int,
    ): MarketListItem {
        val market =
            MarketVO(
                baseCurrencyCode = "BTC",
                quoteCurrencyCode = quoteCode,
                baseCurrencyName = "Bitcoin",
                quoteCurrencyName = quoteName,
            )
        return MarketListItem.from(market = market, numOffers = numOffers)
    }

    private class FakeMarketPriceServiceFacade(
        settingsRepository: SettingsRepository,
        private val marketsWithPrice: Set<String>,
    ) : MarketPriceServiceFacade(settingsRepository) {
        override fun findMarketPriceItem(marketVO: MarketVO): MarketPriceItem? {
            if (!marketsWithPrice.contains(marketVO.quoteCurrencyCode)) return null
            val quote = PriceQuoteVOFactory.run { fromPrice(priceValue = 100_00L, market = marketVO) }
            return MarketPriceItem(marketVO, quote, formattedPrice = "100")
        }

        override fun findUSDMarketPriceItem(): MarketPriceItem? = findMarketPriceItem(MarketVO("BTC", "USD"))

        override fun refreshSelectedFormattedMarketPrice() {
        }

        override fun selectMarket(marketListItem: MarketListItem): Result<Unit> = Result.success(Unit)
    }
}
