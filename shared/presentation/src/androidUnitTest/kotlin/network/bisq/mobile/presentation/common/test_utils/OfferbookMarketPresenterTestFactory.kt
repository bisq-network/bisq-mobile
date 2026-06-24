package network.bisq.mobile.presentation.common.test_utils

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.data.model.offerbook.MarketListItem
import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.data.service.offers.OffersServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.repository.SettingsRepository
import network.bisq.mobile.presentation.tabs.offers.OfferbookMarketPresenter
import network.bisq.mobile.presentation.tabs.offers.usecase.ComputeOfferbookMarketListUseCase

object OfferbookMarketPresenterTestFactory {
    fun create(
        settingsRepository: SettingsRepository,
        offersServiceFacade: OffersServiceFacade =
            mockk<OffersServiceFacade>(relaxed = true).also {
                every { it.offerbookMarketItems } returns MutableStateFlow(emptyList())
            },
        marketPriceServiceFacade: MarketPriceServiceFacade =
            object : MarketPriceServiceFacade(settingsRepository) {
                override fun findMarketPriceItem(marketVO: MarketVO) = null

                override fun findUSDMarketPriceItem() = null

                override fun refreshSelectedFormattedMarketPrice() {
                }

                override fun selectMarket(marketListItem: MarketListItem): Result<Unit> = Result.success(Unit)
            },
        computationDispatcher: CoroutineDispatcher = Dispatchers.Default,
    ): OfferbookMarketPresenter {
        val mainPresenter =
            MainPresenterTestFactory.create(applicationLifecycleService = TestApplicationLifecycleService())

        val userProfileServiceFacade = mockk<UserProfileServiceFacade>(relaxed = true)
        every { userProfileServiceFacade.ignoredProfileIds } returns MutableStateFlow(emptySet())

        return OfferbookMarketPresenter(
            mainPresenter = mainPresenter,
            offersServiceFacade = offersServiceFacade,
            marketPriceServiceFacade = marketPriceServiceFacade,
            userProfileServiceFacade = userProfileServiceFacade,
            settingsRepository = settingsRepository,
            computeOfferbookMarketListUseCase = ComputeOfferbookMarketListUseCase(marketPriceServiceFacade),
            computationDispatcher = computationDispatcher,
        )
    }
}
