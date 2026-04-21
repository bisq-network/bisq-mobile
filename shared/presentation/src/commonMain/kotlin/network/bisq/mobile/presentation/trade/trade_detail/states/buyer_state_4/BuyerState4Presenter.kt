package network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_4

import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.repository.TradeReadStateRepository
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.share.ShareFileService
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.trade.trade_detail.states.common.State4Presenter

class BuyerState4Presenter(
    mainPresenter: MainPresenter,
    tradesServiceFacade: TradesServiceFacade,
    tradeReadStateRepository: TradeReadStateRepository,
    shareFileService: ShareFileService,
) : State4Presenter(mainPresenter, tradesServiceFacade, tradeReadStateRepository, shareFileService) {
    override fun resolveMyDirectionLabel(): String {
        return "bisqEasy.tradeCompleted.header.myDirection.buyer".i18n() // I bought
    }

    override fun resolveMyOutcomeLabel(): String {
        return "bisqEasy.tradeCompleted.header.myOutcome.buyer".i18n() // I paid
    }
}
