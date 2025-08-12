package network.bisq.mobile.presentation.ui.uicases

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.domain.data.model.offerbook.MarketListItem
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.network_stats.ProfileStatsServiceFacade
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.BisqLinks
import network.bisq.mobile.presentation.ui.navigation.Routes
import kotlin.time.Duration.Companion.seconds

open class DashboardPresenter(
    private val mainPresenter: MainPresenter,
    private val profileStatsServiceFacade: ProfileStatsServiceFacade,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val offersServiceFacade: OffersServiceFacade
) : BasePresenter(mainPresenter), IGettingStarted {
    override val titleKey: String = "mobile.dashboard.title"

    override val bulletPointsKey: List<String> = listOf(
        "mobile.dashboard.bulletPoint1",
        "mobile.dashboard.bulletPoint2",
        "mobile.dashboard.bulletPoint3",
    )

    companion object {
        private val OFFERS_UPDATE_THROTTLE = 1.seconds
        private val PROFILES_UPDATE_THROTTLE = 2.seconds
    }

    private val _pendingOffersCount = MutableStateFlow<Int?>(null)
    private val _offersOnline = MutableStateFlow(0)
    override val offersOnline: StateFlow<Int> get() = _offersOnline.asStateFlow()

    private val _pendingProfilesCount = MutableStateFlow<Int?>(null)
    private val _publishedProfiles = MutableStateFlow(0)
    override val publishedProfiles: StateFlow<Int> get() = _publishedProfiles.asStateFlow()

    private var offersThrottleJob: Job? = null
    private var profilesThrottleJob: Job? = null

    val formattedMarketPrice: StateFlow<String> get() = marketPriceServiceFacade.selectedFormattedMarketPrice

    override fun onViewAttached() {
        super.onViewAttached()

        launchThrottledOfferCountUpdateJob()
        launchThrottledProfileCountJob()
        launchLanguageDependentUpdatesJob()
    }

    private fun launchLanguageDependentUpdatesJob() {
        collectUI(mainPresenter.languageCode) {
            marketPriceServiceFacade.refreshSelectedFormattedMarketPrice()
        }
    }

    private fun launchThrottledProfileCountJob() {
        collectUI(profileStatsServiceFacade.publishedProfilesCount) { count ->
            _pendingProfilesCount.value = count

            profilesThrottleJob?.cancel()
            profilesThrottleJob = launchUI {
                delay(PROFILES_UPDATE_THROTTLE)
                _pendingProfilesCount.value?.let { profileCount ->
                    _publishedProfiles.value = profileCount
                    log.d { "DashboardPresenter: NetworkStats publishedProfilesCount received (throttled): $profileCount" }
                }
            }
        }
    }

    private fun launchThrottledOfferCountUpdateJob() {
        collectUI(offersServiceFacade.offerbookMarketItems) { items ->
            val totalOffers = items.sumOf { it.numOffers }
            _pendingOffersCount.value = totalOffers

            launchThrottledOffersJob(items)
        }
    }

    private fun launchThrottledOffersJob(items: List<MarketListItem>) {
        offersThrottleJob?.cancel()
        offersThrottleJob = launchUI {
            delay(OFFERS_UPDATE_THROTTLE)
            _pendingOffersCount.value?.let { count ->
                _offersOnline.value = count
                log.d { "DashboardPresenter: Updated offers online count (throttled): $count (items: ${items.size})" }
            }
        }
    }

    override fun onStartTrading() {
        disableInteractive()
        navigateToTradingTab()
        enableInteractive()
    }

    private fun navigateToTradingTab() {
        navigateToTab(Routes.TabOfferbook)
    }

    override fun navigateLearnMore() {
        navigateToUrl(BisqLinks.BISQ_EASY_WIKI_URL)
    }
}