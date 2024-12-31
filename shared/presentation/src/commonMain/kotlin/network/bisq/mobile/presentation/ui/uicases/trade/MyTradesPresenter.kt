package network.bisq.mobile.presentation.ui.uicases.trade

import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.data.model.MockOffer
import network.bisq.mobile.domain.data.repository.MyTradesRepository
import network.bisq.mobile.domain.replicated.offer.bisq_easy.OfferListItemVO
import network.bisq.mobile.domain.service.offerbook.OfferbookServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

class MyTradesPresenter(
    mainPresenter: MainPresenter,
    private val offerbookServiceFacade: OfferbookServiceFacade,
    // private val myTradesRepository: MyTradesRepository,
) : BasePresenter(mainPresenter), IMyTrades {

    private val _myTrades = MutableStateFlow<List<MockOffer>>(emptyList())
    override val myTrades: StateFlow<List<MockOffer>> = _myTrades

    override fun navigateToCurrencyList() {
        val tabController = getRootTabNavController()
        tabController.navigate(Routes.TabCurrencies.name) {
            tabController.graph.startDestinationRoute?.let { route ->
                popUpTo(route) {
                    saveState = true
                }
            }
            launchSingleTop = true
            restoreState = true
        }
    }


    override fun createOffer() {
        log.i { "Goto create offer" }
        rootNavigator.navigate(Routes.CreateOfferDirection.name)
    }

    override fun gotoTradeScreen(offer: MockOffer) {
        log.i { "Goto trade screen" }
        rootNavigator.navigate(Routes.TradeFlow.name)
    }

    private fun refresh() {
//        CoroutineScope(BackgroundDispatcher).launch {
//            try {
//                delay(1000) // TODO: To simulate loading. Yet to be handled
//                val trades = myTradesRepository.fetch()
//                _myTrades.value = trades?.trades ?: emptyList()
//            } catch (e: Exception) {
//                // Handle errors
//                println("Error: ${e.message}")
//            }
//        }
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