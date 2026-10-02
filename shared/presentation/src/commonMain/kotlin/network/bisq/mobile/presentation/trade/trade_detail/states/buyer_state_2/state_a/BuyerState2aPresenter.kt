package network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_2.state_a

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.main.MainPresenter

class BuyerState2aPresenter(
    mainPresenter: MainPresenter,
    private val tradesServiceFacade: TradesServiceFacade,
    private val userProfileServiceFacade: UserProfileServiceFacade,
) : BasePresenter(mainPresenter) {
    private val _uiState = MutableStateFlow(BuyerState2aUiState())
    val uiState: StateFlow<BuyerState2aUiState> = _uiState.asStateFlow()

    // Guard for guardedSuspendAction only; the rendered enabled state also depends on the banned check.
    private val confirmGuard = MutableStateFlow(true)

    // Stays false while a cancel is in flight, so a re-attach cannot start a second one.
    private val cancelGuard = MutableStateFlow(true)

    // One report per trade on this instance; reopening the trade screen creates a new one and reports again.
    private val reportedTradeIds = mutableSetOf<String>()

    override fun onViewAttached() {
        super.onViewAttached()
        // A confirmed trade leaves the guard closed; the next trade on this presenter needs it open.
        confirmGuard.value = true
        combine(accountDataChecks(), confirmGuard) { check, guardEnabled -> render(check, guardEnabled) }
            .launchIn(presenterScope)
    }

    override fun onViewUnattaching() {
        // A later attach must not start from another trade's state.
        _uiState.value = BuyerState2aUiState()
        super.onViewUnattaching()
    }

    fun onAction(action: BuyerState2aUiAction) {
        when (action) {
            BuyerState2aUiAction.OnConfirmFiatSent -> onConfirmFiatSent()
            BuyerState2aUiAction.OnAcknowledgeBannedWarning -> onAcknowledgeBannedWarning()
        }
    }

    /** Emits the resolved banned check for the selected trade's account data; null while no trade or while the check is pending. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun accountDataChecks(): Flow<AccountDataCheck?> =
        tradesServiceFacade.selectedTrade
            .flatMapLatest { trade ->
                trade
                    ?.bisqEasyTradeModel
                    ?.paymentAccountData
                    ?.transformLatest<String?, AccountDataCheck?> { data ->
                        // Null first: the previous trade's state must not stay confirmable while this check runs.
                        emit(null)
                        emit(AccountDataCheck(trade, data, data != null && tradesServiceFacade.isAccountDataBanned(data)))
                    }?.retryWhen { cause, attempt ->
                        // A failed check must not end the collector; confirm stays disabled until it answers.
                        if (attempt == 0L) {
                            log.e(cause) { "Banned account data check failed, retrying" }
                        } else {
                            // No stack trace on later attempts: a persistent failure would log one every few seconds.
                            log.w { "Banned account data check still failing, attempt ${attempt + 1}" }
                        }
                        delay((CHECK_RETRY_DELAY_MS * (attempt + 1)).coerceAtMost(CHECK_RETRY_MAX_DELAY_MS))
                        true
                    } ?: flowOf(null)
            }.onEach { check -> if (check?.isBanned == true) onBannedAccountData(check) }

    private fun render(
        check: AccountDataCheck?,
        confirmGuardEnabled: Boolean,
    ) {
        _uiState.update {
            it.copy(
                isTradeLoaded = check != null,
                quoteAmountWithCode = check?.trade?.quoteAmountWithCode.orEmpty(),
                paymentAccountData = check?.accountData,
                tradeShortId =
                    check
                        ?.trade
                        ?.bisqEasyTradeModel
                        ?.shortId
                        .orEmpty(),
                isConfirmFiatSentEnabled = confirmGuardEnabled && check?.accountData != null && !check.isBanned,
                isAccountDataBanned = check?.isBanned == true,
                isBannedWarningVisible = it.isBannedWarningVisible && check?.isBanned == true,
            )
        }
    }

    private fun onBannedAccountData(check: AccountDataCheck) {
        val trade = check.trade
        _uiState.update { it.copy(isBannedWarningVisible = true) }
        if (!reportedTradeIds.add(trade.tradeId)) return
        presenterScope.launch {
            // Desktop's moderator message, not user facing.
            val message = "Account data of ${trade.peersUserName} is banned: ${check.accountData}"
            // NonCancellable: the report must go out even if the buyer leaves the screen.
            withContext(NonCancellable) { userProfileServiceFacade.reportUserProfile(trade.peersUserProfile, message) }
                .onFailure { log.e(it) { "Failed to report peer with banned account data" } }
        }
    }

    private fun onAcknowledgeBannedWarning() {
        val state = _uiState.value
        if (!state.isAccountDataBanned || !state.isBannedWarningVisible) return
        _uiState.update { it.copy(isBannedWarningVisible = false) }
        guardedSuspendAction(cancelGuard, "onAcknowledgeBannedWarning", showLoadingOverlay = false) {
            // NonCancellable: the cancel itself detaches this presenter, so a failure must still be shown.
            withContext(NonCancellable) { tradesServiceFacade.cancelTradeForBannedAccountData() }
                .onFailure { handleError(it) }
        }
    }

    private fun onConfirmFiatSent() {
        if (!_uiState.value.isConfirmFiatSentEnabled) return
        guardedSuspendAction(
            confirmGuard,
            "onConfirmFiatSent",
            reEnableGuardOnComplete = false,
        ) {
            tradesServiceFacade.buyerConfirmFiatSent().onFailure {
                confirmGuard.value = true
            }
        }
    }

    private companion object {
        const val CHECK_RETRY_DELAY_MS = 1_000L
        const val CHECK_RETRY_MAX_DELAY_MS = 10_000L
    }

    private data class AccountDataCheck(
        val trade: TradeItemPresentationModel,
        val accountData: String?,
        val isBanned: Boolean,
    )
}
