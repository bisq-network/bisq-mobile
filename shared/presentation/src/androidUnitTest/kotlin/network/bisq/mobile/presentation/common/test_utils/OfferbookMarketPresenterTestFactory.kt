package network.bisq.mobile.presentation.common.test_utils

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.domain.data.model.offerbook.MarketListItem
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.tabs.offers.OfferbookMarketPresenter
import network.bisq.mobile.presentation.tabs.offers.usecase.ComputeOfferbookMarketListUseCase

object OfferbookMarketPresenterTestFactory {
    fun create(
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
}
