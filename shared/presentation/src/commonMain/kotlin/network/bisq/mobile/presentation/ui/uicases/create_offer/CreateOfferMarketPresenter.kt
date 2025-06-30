package network.bisq.mobile.presentation.ui.uicases.create_offer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import network.bisq.mobile.domain.data.model.offerbook.MarketListItem
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnumExtensions.isBuy
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

class CreateOfferMarketPresenter(
    mainPresenter: MainPresenter,
    private val offersServiceFacade: OffersServiceFacade,
    private val createOfferPresenter: CreateOfferPresenter
) : BasePresenter(mainPresenter) {

    lateinit var headline: String
    var market: MarketVO? = null
    var marketListItem: MarketListItem? = null

    private var _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText
    fun setSearchText(newValue: String) {
        _searchText.value = newValue
    }

    private val _marketList = MutableStateFlow(offersServiceFacade.getSortedOfferbookMarketItems())

    val marketListItemWithNumOffers: StateFlow<List<MarketListItem>> = combine(
        _searchText,
        _marketList
    ) { searchText, marketList ->
        if (searchText.isBlank()) {
            marketList
        } else {
            marketList.filter {
                it.market.quoteCurrencyCode.contains(searchText, ignoreCase = true) ||
                        it.market.quoteCurrencyName.contains(searchText, ignoreCase = true)
            }
        }
    }.stateIn(
        CoroutineScope(Dispatchers.Main),
        SharingStarted.Lazily,
        emptyList()
    )

    init {
        val createOfferModel = createOfferPresenter.createOfferModel
        market = createOfferModel.market

        headline = if (createOfferModel.direction.isBuy)
            "mobile.bisqEasy.tradeWizard.market.headline.buyer".i18n()
        else
            "mobile.bisqEasy.tradeWizard.market.headline.seller".i18n()

        //todo for dev testing
        /* if (market == null) {
             market = marketListItemWithNumOffers[0].market
         }*/
    }

    fun onSelectMarket(_marketListItem: MarketListItem) {
        marketListItem = _marketListItem
        market = _marketListItem.market
        navigateNext()
    }

    fun onBack() {
        commitToModel()
        navigateBack()
    }

    fun onNext() {
        if (isValid()) {
            navigateNext()
        }
    }

    private fun navigateNext() {
        commitToModel()
        navigateTo(Routes.CreateOfferAmount)
    }

    private fun commitToModel() {
        if (isValid()) {
            runCatching {
                createOfferPresenter.commitMarket(market!!)
                offersServiceFacade.selectOfferbookMarket(marketListItem!!)
            }.onFailure {
                log.e(it) { "Failed to comit to model ${it.message}" }
            }
        }
    }

    private fun isValid() = market != null
}
