package network.bisq.mobile.domain.service.trades

import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.domain.utils.Logging

/**
 * Shared helper class for trade synchronization logic across node and client implementations.
 * 
 * **Purpose**: Provides consistent trade synchronization timing and logic for both
 * androidNode and client implementations to ensure missed trade state updates are
 * detected and synchronized properly.
 * 
 * **Timing Strategy**:
 * - Quick-progress states: sync after 30 seconds
 * - All ongoing trades: sync after 60 seconds  
 * - Long-running trades: sync after 5 minutes
 */
object TradeSynchronizationHelper : Logging {

    /**
     * Determines if a trade should be synchronized based on its state and timing.
     *
     * @param trade The trade to evaluate for synchronization
     * @param isAppRestart Whether this is being called during app restart (default: false)
     * @return true if the trade should be synchronized, false otherwise
     */
    fun shouldSynchronizeTrade(trade: TradeItemPresentationModel, isAppRestart: Boolean = false): Boolean {
        val currentState = trade.bisqEasyTradeModel.tradeState.value
        val timeSinceCreation = System.currentTimeMillis() - trade.bisqEasyTradeModel.takeOfferDate

        // On app restart, sync ALL non-final trades to ensure we haven't missed state changes
        if (isAppRestart) {
            log.d { "KMP: Trade ${trade.shortTradeId} should sync - app restart, age: ${timeSinceCreation / 1000}s" }
            return true
        }

        // Always sync trades that have been open for more than 5 minutes
        if (timeSinceCreation > 5 * 60 * 1000) {
            log.d { "KMP: Trade ${trade.shortTradeId} should sync - open for ${timeSinceCreation / 60000} minutes" }
            return true
        }

        // Sync ongoing trades after 60 seconds
        if (timeSinceCreation > 60 * 1000) {
            log.d { "KMP: Trade ${trade.shortTradeId} should sync - ongoing trade open for ${timeSinceCreation / 1000} seconds" }
            return true
        }

        // Sync trades in quick-progress states after 30 seconds
        if (isQuickProgressState(currentState) && timeSinceCreation > 30 * 1000) {
            log.d { "KMP: Trade ${trade.shortTradeId} should sync - quick-progress state $currentState for ${timeSinceCreation / 1000} seconds" }
            return true
        }

        return false
    }

    /**
     * Checks if a trade state is considered a quick-progress state that should be
     * synchronized more frequently.
     */
    private fun isQuickProgressState(state: BisqEasyTradeStateEnum): Boolean {
        val quickProgressStates = setOf(
            BisqEasyTradeStateEnum.SELLER_SENT_BTC_SENT_CONFIRMATION,
            BisqEasyTradeStateEnum.BUYER_RECEIVED_BTC_SENT_CONFIRMATION,
            BisqEasyTradeStateEnum.SELLER_CONFIRMED_FIAT_RECEIPT,
            BisqEasyTradeStateEnum.BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION
        )
        return quickProgressStates.contains(state)
    }
    
    /**
     * Filters a list of trades to only those that need synchronization.
     *
     * @param trades List of trades to filter
     * @param isAppRestart Whether this is being called during app restart (default: false)
     * @return List of trades that should be synchronized
     */
    fun getTradesNeedingSync(trades: List<TradeItemPresentationModel>, isAppRestart: Boolean = false): List<TradeItemPresentationModel> {
        return trades.filter { trade ->
            // Only consider non-final trades
            if (trade.bisqEasyTradeModel.tradeState.value.isFinalState) {
                return@filter false
            }

            shouldSynchronizeTrade(trade, isAppRestart)
        }
    }
    
    /**
     * Logs synchronization activity for debugging purposes.
     */
    fun logSynchronizationActivity(trades: List<TradeItemPresentationModel>, tradesNeedingSync: List<TradeItemPresentationModel>) {
        log.i { "KMP: Trade synchronization check - ${trades.size} total trades, ${tradesNeedingSync.size} need sync" }
        
        if (tradesNeedingSync.isNotEmpty()) {
            tradesNeedingSync.forEach { trade ->
                val timeSinceCreation = System.currentTimeMillis() - trade.bisqEasyTradeModel.takeOfferDate
                log.i { "KMP: Trade ${trade.shortTradeId} needs sync - state: ${trade.bisqEasyTradeModel.tradeState.value}, age: ${timeSinceCreation / 1000}s" }
            }
        }
    }
}
