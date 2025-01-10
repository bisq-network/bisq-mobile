package network.bisq.mobile.presentation.ui.uicases.offer.create_offer

import network.bisq.mobile.domain.data.model.MarketListItem
import network.bisq.mobile.domain.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.replicated.offer.isBuy
import network.bisq.mobile.domain.service.offerbook.OfferbookServiceFacade
import network.bisq.mobile.i18n.AppStrings
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

class CreateOfferMarketPresenter(
    mainPresenter: MainPresenter,
    offerbookServiceFacade: OfferbookServiceFacade,
    private val createOfferPresenter: CreateOfferPresenter
) : BasePresenter(mainPresenter) {

    lateinit var appStrings: AppStrings
    lateinit var headline: String
    var market: MarketVO? = null

    var marketListItemWithNumOffers: List<MarketListItem> = offerbookServiceFacade.getSortedOfferbookMarketItems()

    override fun onViewAttached() {
        val createOfferModel = createOfferPresenter.createOfferModel
        market = createOfferModel.market

        headline = if (createOfferModel.direction.isBuy)
            appStrings.bisqEasyTradeWizard.bisqEasy_tradeWizard_market_headline_buyer
        else
            appStrings.bisqEasyTradeWizard.bisqEasy_tradeWizard_market_headline_seller

        //todo for dev testing
        /* if (market == null) {
             market = marketListItemWithNumOffers[0].market
         }*/
    }

    fun onSelectMarket(marketListItem: MarketListItem) {
        market = marketListItem.market
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
            createOfferPresenter.commitMarket(market!!)
        }
    }

    private fun isValid() = market != null
}
