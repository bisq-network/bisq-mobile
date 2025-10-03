package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states

import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.BUYER_STATE1A
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.BUYER_STATE1B
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.BUYER_STATE2A
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.BUYER_STATE2B
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.BUYER_STATE3A
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.BUYER_STATE4
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.BUYER_STATE_LIGHTNING3B
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.BUYER_STATE_MAIN_CHAIN3B
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.INIT
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.SELLER_STATE1
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.SELLER_STATE2A
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.SELLER_STATE2B
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.SELLER_STATE3A
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.SELLER_STATE4
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.SELLER_STATE_LIGHTNING3B
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.SELLER_STATE_MAIN_CHAIN3B

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
    val buyerState2aPresenter: BuyerState2aPresenter,
    val buyerState2bPresenter: BuyerState2bPresenter,
    val buyerState3aPresenter: BuyerState3aPresenter,
    val buyerStateMainChain3bPresenter: BuyerStateMainChain3bPresenter,
    val buyerStateLightning3bPresenter: BuyerStateLightning3bPresenter,
    val buyerState4Presenter: BuyerState4Presenter,
) {

    /**
     * @return the presenter corresponding to the given phase
     * @throws IllegalArgumentException if no presenter is available for the given phase
     */
    fun presenterForPhase(phase: TradeFlowPresenter.TradePhaseState): BasePresenter {
        return (when (phase) {
            INIT, SELLER_STATE1 -> sellerState1Presenter
            SELLER_STATE2A -> sellerState2aPresenter
            SELLER_STATE2B -> sellerState2bPresenter
            SELLER_STATE3A -> sellerState3aPresenter
            SELLER_STATE_MAIN_CHAIN3B -> sellerStateMainChain3bPresenter
            SELLER_STATE_LIGHTNING3B -> sellerStateLightning3bPresenter
            SELLER_STATE4 -> sellerState4Presenter
            BUYER_STATE1A -> buyerState1aPresenter
            BUYER_STATE1B -> buyerState1aPresenter // not used
            BUYER_STATE2A -> buyerState2aPresenter
            BUYER_STATE2B -> buyerState2bPresenter
            BUYER_STATE3A -> buyerState3aPresenter
            BUYER_STATE_MAIN_CHAIN3B -> buyerStateMainChain3bPresenter
            BUYER_STATE_LIGHTNING3B -> buyerStateLightning3bPresenter
            BUYER_STATE4 -> buyerState4Presenter
            else -> throw IllegalArgumentException("No presenter for the given phase $phase")
        })
    }
}