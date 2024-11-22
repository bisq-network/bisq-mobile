package network.bisq.mobile.presentation.ui.uicases.exchange

import androidx.navigation.NavController
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.currency_usd
import co.touchlab.kermit.Logger
import network.bisq.mobile.client.replicated_model.common.currency.Market
import network.bisq.mobile.domain.offerbook.OfferbookServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.uicases.exchange.IconMap.Companion.ICON_BY_CODE

class ExchangePresenter(
    mainPresenter: MainPresenter,
    navController: NavController,
    val service: OfferbookServiceFacade,
) : BasePresenter(mainPresenter) {

    private val log = Logger.withTag(this::class.simpleName ?: "ExchangePresenter")
    private var mainCurrencies = OfferbookServiceFacade.mainCurrencies

    var marketWithNumOffers: List<Market> = service.markets
        .sortedWith(
            compareByDescending<Market> {  it.numOffers.value }
                .thenByDescending { mainCurrencies.contains(it.quoteCurrencyCode.lowercase()) } // [1]
                .thenBy { item->
                    if (!mainCurrencies.contains(item.quoteCurrencyCode.lowercase())) item.quoteCurrencyName
                    else null // Null values will naturally be sorted together
                }
        )
    // [1] thenBy doesn’t work as expected for boolean expressions because true and false are
    // sorted alphabetically (false before true), thus we use thenByDescending

    fun drawableResource(code: String) =
        ICON_BY_CODE[code.lowercase()] ?: Res.drawable.currency_usd

    override fun onViewAttached() {
    }

    override fun onResume() {
        service.resume()
    }

    override fun onPause() {
        service.dispose()
    }

    override fun onViewUnattaching() {
        service.dispose()
    }

    override fun onDestroying() {
        service.dispose()
    }
}
