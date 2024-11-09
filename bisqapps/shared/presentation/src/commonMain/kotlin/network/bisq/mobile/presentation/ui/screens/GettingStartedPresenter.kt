package network.bisq.mobile.presentation.ui.screens

import PriceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.repository.BisqStatsRepository
import network.bisq.mobile.presentation.BasePresenter

/**
 * Main Presenter as an example of implementation for now.
 */
class GettingStartedPresenter(private val priceRepository: PriceRepository, private val bisqStatsRepository: BisqStatsRepository) : BasePresenter(), IGettingStarted {
    private val _btcPrice = MutableStateFlow("Loading...")//("$75,000")
    override val btcPrice: StateFlow<String> = _btcPrice

    private val _offersOnline = MutableStateFlow(145)
    override val offersOnline: StateFlow<Number> = _offersOnline

    private val _publishedProfiles = MutableStateFlow(1145)
    override val publishedProfiles: StateFlow<Number> = _publishedProfiles

    fun refresh() {
        _btcPrice.value = priceRepository.getValue()
        _offersOnline.value = bisqStatsRepository.getOffersOnline()
        _publishedProfiles.value = bisqStatsRepository.getPublishedProfiles()

//        presenterScope.launch {
//            val price = priceRepository.fetchBtcPrice()
//            _btcPrice.value = price
//        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }
}