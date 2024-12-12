package network.bisq.mobile.presentation.ui.uicases

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.repository.BisqStatsRepository
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

class GettingStartedPresenter(
    mainPresenter: MainPresenter,
    private val bisqStatsRepository: BisqStatsRepository,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
) : BasePresenter(mainPresenter), IGettingStarted {

    private val _offersOnline = MutableStateFlow(145)
    override val offersOnline: StateFlow<Int> = _offersOnline

    private val _publishedProfiles = MutableStateFlow(1145)
    override val publishedProfiles: StateFlow<Int> = _publishedProfiles

    val formattedMarketPrice: StateFlow<String> = marketPriceServiceFacade.selectedFormattedMarketPrice

    private var job: Job? = null

    override fun navigateToCreateOffer() {
        rootNavigator.navigate(Routes.CreateOfferBuySell.name)
        // rootNavigator.navigate(Routes.CreateOfferReviewOffer.name)
    }

    private fun refresh() {
        job = backgroundScope.launch {
            try {
                val bisqStats = bisqStatsRepository.fetch()
                _offersOnline.value = bisqStats?.offersOnline ?: 0
                _publishedProfiles.value = bisqStats?.publishedProfiles ?: 0
            } catch (e: Exception) {
                // Handle errors
                println("Error: ${e.message}")
            }
        }
    }

    override fun onViewAttached() {
        super.onViewAttached()
        refresh()
    }

    override fun onViewUnattaching() {
        super.onViewUnattaching()
        job?.cancel()
        job = null
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }
}