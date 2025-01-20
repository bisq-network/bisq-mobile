package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states

import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter

/**
 * A group class to join all related trade states presenters
 */
class TradeStatesProvider(
    val sellerState1Presenter: SellerState1Presenter,
    val sellerState2aPresenter: SellerState2aPresenter,
    val sellerState2bPresenter: SellerState2bPresenter,
    val sellerState3aPresenter: SellerState3aPresenter,
    val sellerStateMainChain3bPresenter: SellerStateMainChain3bPresenter,
    val sellerStateLightning3bPresenter: SellerStateLightning3bPresenter,
    val sellerState4Presenter: SellerState4Presenter,

    val buyerState1aPresenter: BuyerState1aPresenter,
    val buyerState2aPresente: BuyerState2aPresenter,
    val buyerState2bPresente: BuyerState2bPresenter,
    val buyerState3aPresenter:BuyerState3aPresenter,
    val buyerStateMainChain3bPresenter: BuyerStateMainChain3bPresenter,
    val buyerStateLightning3bPresenter: BuyerStateLightning3bPresenter,
    val buyerState4Presenter: BuyerState4Presenter,
) {

    /**
     * @return the presenter correspoding to the given phase
     * @throws IllegalArgumentException if no presenter is available for the given phase
     */
    fun presenterForPhase(phase: TradeFlowPresenter.TradePhaseState): BasePresenter {
        return (when(phase) {
            TradeFlowPresenter.TradePhaseState.SELLER_STATE1 -> sellerState1Presenter
            TradeFlowPresenter.TradePhaseState.SELLER_STATE2A -> sellerState2aPresenter
            TradeFlowPresenter.TradePhaseState.SELLER_STATE2B -> sellerState2bPresenter
            TradeFlowPresenter.TradePhaseState.SELLER_STATE3A -> sellerState3aPresenter
            TradeFlowPresenter.TradePhaseState.SELLER_STATE_MAIN_CHAIN3B -> sellerStateMainChain3bPresenter
            TradeFlowPresenter.TradePhaseState.SELLER_STATE_LIGHTNING3B -> sellerStateLightning3bPresenter
            TradeFlowPresenter.TradePhaseState.SELLER_STATE4 -> sellerState4Presenter
            TradeFlowPresenter.TradePhaseState.BUYER_STATE1A -> buyerState1aPresenter
            TradeFlowPresenter.TradePhaseState.BUYER_STATE2A -> buyerState2aPresente
            TradeFlowPresenter.TradePhaseState.BUYER_STATE2B -> buyerState2bPresente
            TradeFlowPresenter.TradePhaseState.BUYER_STATE3A -> buyerState3aPresenter
            TradeFlowPresenter.TradePhaseState.BUYER_STATE_MAIN_CHAIN3B -> buyerStateMainChain3bPresenter
            TradeFlowPresenter.TradePhaseState.BUYER_STATE_LIGHTNING3B -> buyerStateLightning3bPresenter
            TradeFlowPresenter.TradePhaseState.BUYER_STATE4 -> buyerState4Presenter
            else -> throw IllegalArgumentException("No presenter for the given phase $phase")
        })
    }


}