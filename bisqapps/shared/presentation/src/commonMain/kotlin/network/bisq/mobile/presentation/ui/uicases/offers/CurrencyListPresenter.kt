package network.bisq.mobile.presentation.ui.uicases.offers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.data.model.FiatCurrency
import network.bisq.mobile.domain.data.repository.CurrenciesRepository
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

class CurrencyListPresenter(
    mainPresenter: MainPresenter,
    private val currenciesRepository: CurrenciesRepository,
) : BasePresenter(mainPresenter), ICurrencyList {

    private val _currencies = MutableStateFlow<List<FiatCurrency>>(emptyList())
    override val currencies: StateFlow<List<FiatCurrency>> = _currencies

    override fun onSelectedCurrency(currency: FiatCurrency) {
        rootNavigator.navigate(Routes.OfferList.name)
    }

    private fun refresh() {
        CoroutineScope(BackgroundDispatcher).launch {
            try {
                delay(1000) // TODO: To simulate loading. Yet to be handled
                val currencies = currenciesRepository.fetch()
                _currencies.value = currencies?.currencies ?: emptyList()
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
