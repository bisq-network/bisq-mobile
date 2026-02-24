package network.bisq.mobile.presentation.tabs.offers

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
import network.bisq.mobile.presentation.common.test_utils.TestCoroutineJobsManager
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

@OptIn(ExperimentalCoroutinesApi::class)
class OfferbookMarketPresenterSettingsPersistenceTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        startKoin {
            modules(
                module {
                    factory<CoroutineJobsManager> { TestCoroutineJobsManager(testDispatcher) }
                    single<NavigationManager> { NoopNavigationManager() }
                },
            )
        }

        // Avoid touching Android-specific density/resources in MainPresenter.init
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

    private fun createPresenter(settingsRepository: SettingsRepository): OfferbookMarketPresenter {
        val mainPresenter =
            MainPresenterTestFactory.create(applicationLifecycleService = TestApplicationLifecycleService())

        val offersServiceFacade = mockk<OffersServiceFacade>(relaxed = true)
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
    fun `changing market sort by from UI persists to SettingsRepository`() =
        runTest(testDispatcher) {
            val settingsRepository = SettingsRepositoryMock()
            val presenter = createPresenter(settingsRepository)

            presenter.setSortBy(MarketSortBy.NameAZ)
            advanceUntilIdle()

            assertEquals(MarketSortBy.NameAZ, settingsRepository.fetch().marketSortBy)
            assertEquals(MarketSortBy.NameAZ, presenter.sortBy.first { it == MarketSortBy.NameAZ })
        }

    @Test
    fun `changing market filter from UI persists to SettingsRepository`() =
        runTest(testDispatcher) {
            val settingsRepository = SettingsRepositoryMock()
            val presenter = createPresenter(settingsRepository)

            presenter.setFilter(MarketFilter.WithOffers)
            advanceUntilIdle()

            assertEquals(MarketFilter.WithOffers, settingsRepository.fetch().marketFilter)
            assertEquals(MarketFilter.WithOffers, presenter.filter.first { it == MarketFilter.WithOffers })
        }
}
