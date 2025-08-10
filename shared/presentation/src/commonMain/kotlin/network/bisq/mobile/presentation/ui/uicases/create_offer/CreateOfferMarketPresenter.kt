package network.bisq.mobile.presentation.ui.uicases.create_offer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import network.bisq.mobile.domain.data.model.offerbook.MarketListItem
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnumExtensions.isBuy
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.uicases.market.MarketFilterUtil

class CreateOfferMarketPresenter(
    mainPresenter: MainPresenter,
    private val offersServiceFacade: OffersServiceFacade,
    private val createOfferPresenter: CreateOfferPresenter,
    private val marketPriceServiceFacade: MarketPriceServiceFacade
) : BasePresenter(mainPresenter) {

    var headline: String
    var market: MarketVO? = null
    private var marketListItem: MarketListItem? = null

    private var _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> get() = _searchText.asStateFlow()
    fun setSearchText(newValue: String) {
        _searchText.value = newValue
    }

    // Trigger to force market list updates when market prices change
    private val _marketPriceUpdated = MutableStateFlow(false)

    override fun onViewAttached() {
        super.onViewAttached()

        // wait for market items to be ready
        disableInteractive()
        collectIO(offersServiceFacade.offerbookMarketItems) { markets ->
            if (markets.isNotEmpty()) {
                enableInteractive()
            }
        }

        observeGlobalMarketPrices()
    }

    private fun observeGlobalMarketPrices() {
        collectIO(marketPriceServiceFacade.globalPriceUpdate) { timestamp ->
            log.d { "CreateOffer received global price update at timestamp: $timestamp" }
            val previousValue = _marketPriceUpdated.value
            _marketPriceUpdated.value = !_marketPriceUpdated.value
            log.d { "CreateOffer triggered market filtering update: $previousValue -> ${_marketPriceUpdated.value}" }
        }
    }

    val marketListItemWithNumOffers: StateFlow<List<MarketListItem>> = combine(
        _searchText,
        offersServiceFacade.offerbookMarketItems,
        _marketPriceUpdated
    ) { searchText, marketList, _ ->
        // Use shared filtering utility for consistent behavior
        MarketFilterUtil.filterAndSortMarketsForCreateOffer(
            marketList,
            searchText,
            marketPriceServiceFacade
        )
    }.stateIn(
        presenterScope,
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

    fun onClose() {
        commitToModel()
        navigateToOfferList()
    }


    fun onNext() {
        if (isValid()) {
            navigateNext()
        }
    }

    private fun navigateToOfferList() {
        navigateBackTo(Routes.TabContainer)
        navigateToTab(Routes.TabOfferbook)
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
