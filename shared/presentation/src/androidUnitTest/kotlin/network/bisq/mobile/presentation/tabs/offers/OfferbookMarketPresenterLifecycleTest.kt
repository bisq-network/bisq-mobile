package network.bisq.mobile.presentation.tabs.offers

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.domain.data.model.MarketFilter
import network.bisq.mobile.domain.data.model.MarketSortBy
import network.bisq.mobile.domain.data.model.offerbook.MarketListItem
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.data.repository.SettingsRepositoryMock
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.presentation.common.test_utils.MainPresenterTestFactory
import network.bisq.mobile.presentation.common.test_utils.NoopNavigationManager
import network.bisq.mobile.presentation.common.test_utils.TestApplicationLifecycleService
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.platform.getScreenWidthDp
import network.bisq.mobile.presentation.tabs.offers.usecase.ComputeOfferbookMarketListUseCase
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A CoroutineJobsManager that recreates scope on dispose(), matching real
 * DefaultCoroutineJobsManager behavior. This is needed to test presenter
 * lifecycle (attach/detach/reattach) correctly.
 */
private class LifecycleAwareTestJobsManager(
    private val dispatcher: kotlinx.coroutines.CoroutineDispatcher,
    override var coroutineExceptionHandler: ((Throwable) -> Unit)? = null,
) : CoroutineJobsManager {
    private var scope = CoroutineScope(dispatcher + SupervisorJob())

    override suspend fun dispose() {
        runCatching { scope.cancel() }
        scope = CoroutineScope(dispatcher + SupervisorJob())
    }

    override fun getScope(): CoroutineScope = scope
}

/**
 * Tests that OfferbookMarketPresenter's filter and sortBy StateFlows survive
 * the tab switch lifecycle (attach -> detach/dispose -> reattach).
 *
 * Regression test for https://github.com/bisq-network/bisq-mobile/issues/1197
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OfferbookMarketPresenterLifecycleTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        startKoin {
            modules(
                module {
                    factory<CoroutineJobsManager> { LifecycleAwareTestJobsManager(testDispatcher) }
                    single<NavigationManager> { NoopNavigationManager() }
                    single { GlobalUiManager(testDispatcher) }
                },
            )
        }

        mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        every { getScreenWidthDp() } returns 480
    }

    @AfterTest
    fun tearDown() {
        try {
            unmockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        } finally {
            Dispatchers.resetMain()
            stopKoin()
        }
    }

    private fun createPresenter(
        settingsRepository: SettingsRepository,
        offersServiceFacade: OffersServiceFacade =
            mockk<OffersServiceFacade>(relaxed = true).also {
                every { it.offerbookMarketItems } returns MutableStateFlow(emptyList())
            },
    ): OfferbookMarketPresenter {
        val mainPresenter =
            MainPresenterTestFactory.create(applicationLifecycleService = TestApplicationLifecycleService())

        val userProfileServiceFacade = mockk<UserProfileServiceFacade>(relaxed = true)
        every { userProfileServiceFacade.ignoredProfileIds } returns MutableStateFlow(emptySet())

        val marketPriceServiceFacade =
            object : MarketPriceServiceFacade(settingsRepository) {
                override fun findMarketPriceItem(marketVO: MarketVO) = null

                override fun findUSDMarketPriceItem() = null

                override fun refreshSelectedFormattedMarketPrice() {
                }

                override fun selectMarket(marketListItem: MarketListItem): Result<Unit> = Result.success(Unit)
            }

        return OfferbookMarketPresenter(
            mainPresenter = mainPresenter,
            offersServiceFacade = offersServiceFacade,
            marketPriceServiceFacade = marketPriceServiceFacade,
            userProfileServiceFacade = userProfileServiceFacade,
            settingsRepository = settingsRepository,
            computeOfferbookMarketListUseCase = ComputeOfferbookMarketListUseCase(marketPriceServiceFacade),
        )
    }

    @Test
    fun `filter survives tab switch lifecycle`() =
        runTest(testDispatcher) {
            val settingsRepository = SettingsRepositoryMock()
            val presenter = createPresenter(settingsRepository)

            // Simulate first tab enter
            presenter.onViewAttached()
            advanceUntilIdle()

            // Change filter
            presenter.setFilter(MarketFilter.WithOffers)
            advanceUntilIdle()

            // Verify filter was persisted and reflected
            assertEquals(MarketFilter.WithOffers, settingsRepository.fetch().marketFilter)
            assertEquals(MarketFilter.WithOffers, presenter.filter.value)

            // Simulate tab leave (disposes scope, creating a new one)
            presenter.onViewUnattaching()
            advanceUntilIdle()

            // Simulate tab re-enter
            presenter.onViewAttached()
            advanceUntilIdle()

            // Filter should still reflect the persisted value
            assertEquals(MarketFilter.WithOffers, presenter.filter.value)

            // Filter should still be changeable after tab switch
            presenter.setFilter(MarketFilter.All)
            advanceUntilIdle()

            assertEquals(MarketFilter.All, presenter.filter.value)
            assertEquals(MarketFilter.All, settingsRepository.fetch().marketFilter)
        }

    @Test
    fun `sortBy survives tab switch lifecycle`() =
        runTest(testDispatcher) {
            val settingsRepository = SettingsRepositoryMock()
            val presenter = createPresenter(settingsRepository)

            // Simulate first tab enter
            presenter.onViewAttached()
            advanceUntilIdle()

            // Change sort
            presenter.setSortBy(MarketSortBy.NameAZ)
            advanceUntilIdle()

            assertEquals(MarketSortBy.NameAZ, settingsRepository.fetch().marketSortBy)
            assertEquals(MarketSortBy.NameAZ, presenter.sortBy.value)

            // Simulate tab leave
            presenter.onViewUnattaching()
            advanceUntilIdle()

            // Simulate tab re-enter
            presenter.onViewAttached()
            advanceUntilIdle()

            // Sort should still reflect persisted value
            assertEquals(MarketSortBy.NameAZ, presenter.sortBy.value)

            // Sort should still be changeable after tab switch
            presenter.setSortBy(MarketSortBy.NameZA)
            advanceUntilIdle()

            assertEquals(MarketSortBy.NameZA, presenter.sortBy.value)
            assertEquals(MarketSortBy.NameZA, settingsRepository.fetch().marketSortBy)
        }

    @Test
    fun `filter reflects DataStore value on reattach`() =
        runTest(testDispatcher) {
            val settingsRepository = SettingsRepositoryMock()
            // Pre-set a non-default filter in DataStore
            settingsRepository.setMarketFilter(MarketFilter.WithOffers)

            val presenter = createPresenter(settingsRepository)

            // Simulate tab enter — should sync from DataStore
            presenter.onViewAttached()
            advanceUntilIdle()

            assertEquals(MarketFilter.WithOffers, presenter.filter.value)
        }

    @Test
    fun `filter and sortBy work through multiple tab switches`() =
        runTest(testDispatcher) {
            val settingsRepository = SettingsRepositoryMock()
            val presenter = createPresenter(settingsRepository)

            // First cycle
            presenter.onViewAttached()
            advanceUntilIdle()
            presenter.setFilter(MarketFilter.WithOffers)
            presenter.setSortBy(MarketSortBy.NameZA)
            advanceUntilIdle()
            presenter.onViewUnattaching()
            advanceUntilIdle()

            // Second cycle
            presenter.onViewAttached()
            advanceUntilIdle()
            assertEquals(MarketFilter.WithOffers, presenter.filter.value)
            assertEquals(MarketSortBy.NameZA, presenter.sortBy.value)
            presenter.onViewUnattaching()
            advanceUntilIdle()

            // Third cycle — should still work
            presenter.onViewAttached()
            advanceUntilIdle()
            assertEquals(MarketFilter.WithOffers, presenter.filter.value)
            assertEquals(MarketSortBy.NameZA, presenter.sortBy.value)

            // Change should still work on third cycle
            presenter.setFilter(MarketFilter.All)
            advanceUntilIdle()
            assertEquals(MarketFilter.All, presenter.filter.value)
        }
}
