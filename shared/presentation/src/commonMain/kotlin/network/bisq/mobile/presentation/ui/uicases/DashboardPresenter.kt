package network.bisq.mobile.presentation.ui.uicases

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.repository.BisqStatsRepository
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

open class DashboardPresenter(
    mainPresenter: MainPresenter,
    private val bisqStatsRepository: BisqStatsRepository,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val offersServiceFacade: OffersServiceFacade
) : BasePresenter(mainPresenter), IGettingStarted {
    override val title: String = "Bisq Easy Client"

    override val bulletPoints: List<String> = listOf(
        "Experience Bisq with the guidance of a trusted friend or connect remotely to your own full node.",
        "Connect to Trusted Nodes: Start trading with confidence by connecting to a trusted Bisq node hosted by someone you trust.",
        "Remote Management for Experts: Manage your trades on the go by connecting securely to your own desktop-based Bisq node, no matter where you are."
    )

    private val _offersOnline = MutableStateFlow(0)
    override val offersOnline: StateFlow<Int> = _offersOnline

    private val _publishedProfiles = MutableStateFlow(0)
    override val publishedProfiles: StateFlow<Int> = _publishedProfiles

    val formattedMarketPrice: StateFlow<String> = marketPriceServiceFacade.selectedFormattedMarketPrice

    private var job: Job? = null

    override fun onStartTrading() {
        disableInteractive()
        navigateToTradingTab()
        enableInteractive()
    }

    private fun navigateToTradingTab() {
        navigateToTab(Routes.TabOfferbook)
    }

    override fun navigateLearnMore() {
        disableInteractive()
        navigateToUrl("https://bisq.wiki/Bisq_Easy")
        enableInteractive()
    }

    fun navigateToGuide() {
        navigateTo(Routes.TradeGuideOverview)
    }

    private fun refresh() {
        disableInteractive()
        job = presenterScope.launch {
            val bisqStats = withContext(IODispatcher) {
                bisqStatsRepository.fetch()
            }
            _publishedProfiles.value = bisqStats?.publishedProfiles ?: 0

            launch {
                offersServiceFacade.offerbookMarketItems.collect {
                    _offersOnline.value = 0
                    it.map { item ->
                        _offersOnline.value += item.numOffers.value
                    }
                }
            }
            enableInteractive()
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
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
}