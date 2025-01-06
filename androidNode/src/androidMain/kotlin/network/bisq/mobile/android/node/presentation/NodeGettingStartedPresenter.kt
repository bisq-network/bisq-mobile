package network.bisq.mobile.android.node.presentation

import network.bisq.mobile.domain.data.repository.BisqStatsRepository
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.offerbook.OfferbookServiceFacade
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.uicases.GettingStartedPresenter
import network.bisq.mobile.presentation.ui.uicases.offer.create_offer.CreateOfferPresenter

class NodeGettingStartedPresenter(
    mainPresenter: MainPresenter,
    bisqStatsRepository: BisqStatsRepository,
    marketPriceServiceFacade: MarketPriceServiceFacade,
    createOfferPresenter: CreateOfferPresenter,
    offerbookServiceFacade: OfferbookServiceFacade
) : GettingStartedPresenter(mainPresenter, bisqStatsRepository, marketPriceServiceFacade,   offerbookServiceFacade) {
    override val title: String = "Bisq Easy Node"
    override val bulletPoints: List<String> = listOf(
        "Take control of your trading experience with the full power of Bisq, now on your mobile.",
        "Your Node, Your Privacy: Operate a fully-featured P2P Bisq Node directly from your mobile. No compromises on privacy & security - just like running Bisq on your desktop.",
        "Click on Start Trading button to browse available offers from other Bisq users or create your own. Request mediation if needed.",
        "Bisq Easy protocol uses seller's reputation which is visible on each offer."
    )
}