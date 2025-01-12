package network.bisq.mobile.presentation.ui.uicases.open_trades.selected

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.BUYER_STATE1A
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.BUYER_STATE1B
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.BUYER_STATE3A
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.BUYER_STATE4
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.BUYER_STATE_LIGHTNING3B
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.BUYER_STATE_MAIN_CHAIN3B
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.SELLER_STATE1
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.SELLER_STATE2A
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.SELLER_STATE2B
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.SELLER_STATE3A
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.SELLER_STATE4
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.SELLER_STATE_LIGHTNING3B
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter.TradePhaseState.SELLER_STATE_MAIN_CHAIN3B
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.BuyerState1a
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.BuyerState1b
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.BuyerState2a
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.BuyerState2b
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.BuyerState3a
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.BuyerState4
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.BuyerStateLightning3b
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.BuyerStateMainChain3b
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.SellerState1
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.SellerState2a
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.SellerState2b
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.SellerState3a
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.SellerState4
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.SellerStateLightning3b
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.SellerStateMainChain3b

@Composable
fun TradeFlowPane(presenter: TradeFlowPresenter) {
    val tradePhaseState by presenter.tradePhaseState.collectAsState()
    RememberPresenterLifecycle(presenter)

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (tradePhaseState == SELLER_STATE4) {
            SellerState4(presenter.sellerState4Presenter)
        } else if (tradePhaseState == BUYER_STATE4) {
            BuyerState4(presenter.buyerState4Presenter)
        } else {
            presenter.steps.forEachIndexed { index, step ->
                val isActive = tradePhaseState.index == index
                val isVisited = index < tradePhaseState.index
                val isOpen = index > tradePhaseState.index
                val isLastIndex = index == presenter.steps.size - 1
                val titleColor = if (isOpen) BisqTheme.colors.grey2 else BisqTheme.colors.light1
                val title = "bisqEasy.tradeState.phase${index + 1}".i18n().uppercase()
                TradeFlowItem(
                    index = index,
                    isVisited = isVisited,
                    isActive = isActive,
                    isLastIndex = isLastIndex,
                ) { modifier ->
                    Column(modifier = modifier) {
                        BisqText.baseRegular(
                            text = title,
                            color = titleColor
                        )

                        AnimatedVisibility(visible = isActive) {
                            when (step) {
                                TradeFlowPresenter.TradeFlowStep.ACCOUNT_DETAILS -> {
                                    when (tradePhaseState) {
                                        SELLER_STATE1 -> SellerState1(presenter.sellerState1Presenter)
                                        BUYER_STATE1A -> BuyerState1a(presenter.buyerState1aPresenter)
                                        BUYER_STATE1B -> BuyerState1b()
                                        else -> {}
                                    }
                                }

                                TradeFlowPresenter.TradeFlowStep.FIAT_PAYMENT -> {
                                    when (tradePhaseState) {
                                        SELLER_STATE2A -> SellerState2a(presenter.sellerState2aPresenter)
                                        SELLER_STATE2B -> SellerState2b(presenter.sellerState2bPresenter)
                                        TradeFlowPresenter.TradePhaseState.BUYER_STATE2A -> BuyerState2a(presenter.buyerState2aPresenter)
                                        TradeFlowPresenter.TradePhaseState.BUYER_STATE2B -> BuyerState2b(presenter.buyerState2bPresenter)
                                        else -> {}
                                    }
                                }

                                TradeFlowPresenter.TradeFlowStep.BITCOIN_TRANSFER -> {
                                    when (tradePhaseState) {
                                        SELLER_STATE3A -> SellerState3a(presenter.sellerState3aPresenter)
                                        SELLER_STATE_MAIN_CHAIN3B -> SellerStateMainChain3b(presenter.sellerStateMainChain3bPresenter)
                                        SELLER_STATE_LIGHTNING3B -> SellerStateLightning3b(presenter.sellerStateLightning3bPresenter)
                                        BUYER_STATE3A -> BuyerState3a(presenter.buyerState3aPresenter)
                                        BUYER_STATE_MAIN_CHAIN3B -> BuyerStateMainChain3b(presenter.buyerStateMainChain3bPresenter)
                                        BUYER_STATE_LIGHTNING3B -> BuyerStateLightning3b(presenter.buyerStateLightning3bPresenter)
                                        else -> {}
                                    }
                                }

                                TradeFlowPresenter.TradeFlowStep.TRADE_COMPLETED -> {
                                    // We hide the steppers once done
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}