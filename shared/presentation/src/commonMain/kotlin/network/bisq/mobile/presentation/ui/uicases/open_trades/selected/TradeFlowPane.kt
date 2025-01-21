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
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.*


@Composable
fun TradeFlowPane(presenter: TradeFlowPresenter) {
    val tradePhaseState by presenter.tradePhaseState.collectAsState()
    RememberPresenterLifecycle(presenter)
    val presenterForPhase = presenter.presenterForPhase(tradePhaseState)
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        when (tradePhaseState) {
            SELLER_STATE4 -> {
                SellerState4(presenter.sellerState4Presenter)
            }
            BUYER_STATE4 -> {
                BuyerState4(presenter.buyerState4Presenter)
            }
            else -> {
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
                                            SELLER_STATE1 -> SellerState1(presenterForPhase as SellerState1Presenter)
                                            BUYER_STATE1A -> BuyerState1a(presenterForPhase as BuyerState1aPresenter)
                                            BUYER_STATE1B -> BuyerState1b() // static screen
                                            else -> {}
                                        }
                                    }

                                    TradeFlowPresenter.TradeFlowStep.FIAT_PAYMENT -> {
                                        when (tradePhaseState) {
                                            SELLER_STATE2A -> SellerState2a(presenterForPhase as SellerState2aPresenter)
                                            SELLER_STATE2B -> SellerState2b(presenterForPhase as SellerState2bPresenter)
                                            TradeFlowPresenter.TradePhaseState.BUYER_STATE2A -> BuyerState2a(presenterForPhase as BuyerState2aPresenter)
                                            TradeFlowPresenter.TradePhaseState.BUYER_STATE2B -> BuyerState2b(presenterForPhase as BuyerState2bPresenter)
                                            else -> {}
                                        }
                                    }

                                    TradeFlowPresenter.TradeFlowStep.BITCOIN_TRANSFER -> {
                                        when (tradePhaseState) {
                                            SELLER_STATE3A -> SellerState3a(presenterForPhase as SellerState3aPresenter)
                                            SELLER_STATE_MAIN_CHAIN3B -> SellerStateMainChain3b(presenterForPhase as SellerStateMainChain3bPresenter)
                                            SELLER_STATE_LIGHTNING3B -> SellerStateLightning3b(presenterForPhase as SellerStateLightning3bPresenter)
                                            BUYER_STATE3A -> BuyerState3a(presenterForPhase as BuyerState3aPresenter)
                                            BUYER_STATE_MAIN_CHAIN3B -> BuyerStateMainChain3b(presenterForPhase as BuyerStateMainChain3bPresenter)
                                            BUYER_STATE_LIGHTNING3B -> BuyerStateLightning3b(presenterForPhase as BuyerStateLightning3bPresenter)
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
}