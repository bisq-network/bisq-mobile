package network.bisq.mobile.presentation.trade.trade_detail

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.bisq.mobile.data.replicated.contract.RoleEnum
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.replicated.trade.bisq_easy.BisqEasyTradeModel
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.data.service.mediation.MediationServiceFacade
import network.bisq.mobile.data.service.offers.MediatorNotAvailableException
import network.bisq.mobile.data.service.trades.TradeNotFoundException
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.repository.TradeReadStateRepository
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType
import network.bisq.mobile.presentation.common.ui.error.GenericErrorHandler
import network.bisq.mobile.presentation.main.MainPresenter

class InterruptedTradePresenter(
    mainPresenter: MainPresenter,
    private val tradesServiceFacade: TradesServiceFacade,
    private val mediationServiceFacade: MediationServiceFacade,
    private val tradeReadStateRepository: TradeReadStateRepository,
) : BasePresenter(mainPresenter) {
    private val selectedTrade: StateFlow<TradeItemPresentationModel?> get() = tradesServiceFacade.selectedTrade

    private val _uiState = MutableStateFlow(InterruptedTradeUiState())
    val uiState: StateFlow<InterruptedTradeUiState> = _uiState.asStateFlow()

    // Guards for guardedSuspendAction; mirrored into uiState while the view is attached.
    private val closeTradeGuard = MutableStateFlow(true)
    private val reportToMediatorGuard = MutableStateFlow(true)

    override fun onViewAttached() {
        super.onViewAttached()
        val trade = requireNotNull(selectedTrade.value)
        trade.bisqEasyTradeModel.tradeState
            .onEach { tradeStateChanged(it) }
            .launchIn(presenterScope)
        trade.bisqEasyOpenTradeChannelModel.isInMediation
            .onEach { isInMediation -> _uiState.update { it.copy(isInMediation = isInMediation) } }
            .launchIn(presenterScope)
        combine(closeTradeGuard, reportToMediatorGuard) { closeEnabled, reportEnabled ->
            _uiState.update { it.copy(isCloseTradeEnabled = closeEnabled, isReportToMediatorEnabled = reportEnabled) }
        }.launchIn(presenterScope)
    }

    override fun onViewUnattaching() {
        reset()
        super.onViewUnattaching()
    }

    fun onAction(action: InterruptedTradeUiAction) {
        when (action) {
            InterruptedTradeUiAction.OnCloseTrade -> onCloseTrade()
            InterruptedTradeUiAction.OnReportToMediator -> onReportToMediator()
            InterruptedTradeUiAction.OnDismissNoMediatorAvailableWarning ->
                _uiState.update { it.copy(isNoMediatorAvailableWarningVisible = false) }
            InterruptedTradeUiAction.OnDismissMediationRequestedDialog ->
                _uiState.update { it.copy(isMediationRequestedDialogVisible = false) }
        }
    }

    private fun tradeStateChanged(state: BisqEasyTradeStateEnum?) {
        reset()
        val trade = selectedTrade.value?.bisqEasyTradeModel ?: return
        when (state) {
            BisqEasyTradeStateEnum.REJECTED,
            BisqEasyTradeStateEnum.PEER_REJECTED,
            BisqEasyTradeStateEnum.CANCELLED,
            BisqEasyTradeStateEnum.PEER_CANCELLED,
            -> showInterruption(trade, state)

            BisqEasyTradeStateEnum.FAILED -> {
                val error = trade.errorMessage.value.orEmpty()
                _uiState.update { it.copy(errorMessage = "mobile.bisqEasy.openTrades.failed".i18n(error)) }
            }

            BisqEasyTradeStateEnum.FAILED_AT_PEER -> {
                val error = trade.peersErrorMessage.value.orEmpty()
                _uiState.update { it.copy(errorMessage = "mobile.bisqEasy.openTrades.failedAtPeer".i18n(error)) }
            }

            // Not interrupted: nothing to show.
            else -> Unit
        }
    }

    private fun showInterruption(
        trade: BisqEasyTradeModel,
        state: BisqEasyTradeStateEnum,
    ) {
        val wasCancelled = state == BisqEasyTradeStateEnum.CANCELLED || state == BisqEasyTradeStateEnum.PEER_CANCELLED
        val makerInterrupted = trade.interruptTradeInitiator.value == RoleEnum.MAKER
        val selfInitiated = makerInterrupted == trade.isMaker

        val info =
            when {
                wasCancelled && selfInitiated -> "bisqEasy.openTrades.cancelled.self".i18n()
                wasCancelled -> "bisqEasy.openTrades.cancelled.peer".i18n()
                selfInitiated -> "bisqEasy.openTrades.rejected.self".i18n()
                else -> "bisqEasy.openTrades.rejected.peer".i18n()
            }
        _uiState.update {
            it.copy(
                interruptedTradeInfo = info,
                isReportToMediatorButtonVisible = wasCancelled && !selfInitiated,
            )
        }
    }

    private fun onCloseTrade() {
        val trade = selectedTrade.value ?: return

        guardedSuspendAction(
            closeTradeGuard,
            "onCloseTrade",
            reEnableGuardOnComplete = false,
        ) {
            val result = tradesServiceFacade.closeTrade(trade.tradeId)
            if (result.isFailure) {
                val msg =
                    when (val exception = result.exceptionOrNull()) {
                        is TradeNotFoundException -> "mobile.openTrades.tradeNotFoundDialog.text".i18n()
                        else -> exception?.message ?: ""
                    }
                GenericErrorHandler.handleGenericError(
                    "mobile.bisqEasy.openTrades.closeTrade.failed".i18n(msg),
                )
                closeTradeGuard.value = true
            } else {
                withContext(Dispatchers.IO) {
                    // On success, clear read state. If this fails, report but still navigate back.
                    runCatching {
                        tradeReadStateRepository.clearId(trade.tradeId)
                    }.onFailure { ex ->
                        GenericErrorHandler.handleGenericError(
                            "mobile.bisqEasy.openTrades.clearReadState.failed".i18n(ex.message ?: ""),
                        )
                    }
                }

                navigateBack()
            }
        }
    }

    private fun onReportToMediator() {
        val trade = selectedTrade.value ?: return

        guardedSuspendAction(reportToMediatorGuard, "onReportToMediator") {
            mediationServiceFacade
                .reportToMediator(trade)
                .onSuccess {
                    _uiState.update { it.copy(isMediationRequestedDialogVisible = true) }
                }.onFailure { error ->
                    when (error) {
                        is MediatorNotAvailableException ->
                            _uiState.update { it.copy(isNoMediatorAvailableWarningVisible = true) }

                        else -> showSnackbar("mobile.error.generic".i18n(), SnackbarType.ERROR)
                    }
                }
        }
    }

    // Clears what the trade state drives; mediation and the action guards keep their own sources.
    private fun reset() {
        _uiState.update {
            it.copy(
                interruptedTradeInfo = null,
                errorMessage = null,
                isReportToMediatorButtonVisible = false,
                isNoMediatorAvailableWarningVisible = false,
                isMediationRequestedDialogVisible = false,
            )
        }
    }
}
