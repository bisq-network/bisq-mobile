package network.bisq.mobile.presentation.ui.uicases

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.data.model.BisqStats
import network.bisq.mobile.domain.data.repository.BisqStatsRepository
import network.bisq.mobile.domain.data.repository.BtcPriceRepository
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter

class GettingStartedPresenter(
    mainPresenter: MainPresenter,
    private val priceRepository: BtcPriceRepository,
    private val bisqStatsRepository: BisqStatsRepository
) : BasePresenter(mainPresenter), IGettingStarted {
    private val _btcPrice = MutableStateFlow("Loading...")//("$75,000")
    override val btcPrice: StateFlow<String> = _btcPrice

    private val _offersOnline = MutableStateFlow(145)
    override val offersOnline: StateFlow<Int> = _offersOnline

    private val _publishedProfiles = MutableStateFlow(1145)
    override val publishedProfiles: StateFlow<Int> = _publishedProfiles

    private fun refresh() {
        CoroutineScope(BackgroundDispatcher).launch {
            try {
                val bisqStats = bisqStatsRepository.fetch()
                _offersOnline.value = bisqStats?.offersOnline ?: 0
                _publishedProfiles.value = bisqStats?.publishedProfiles ?: 0

                val btcPrice = priceRepository.fetch()
                val priceList = btcPrice?.prices
                _btcPrice.value = (priceList?.get("USD") ?: 0).toString()
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

    override fun onResume() {
        super.onResume()
        refresh()
    }
}