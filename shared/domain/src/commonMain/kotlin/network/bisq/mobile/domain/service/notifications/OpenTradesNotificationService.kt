package network.bisq.mobile.domain.service.notifications

import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.domain.service.notifications.controller.NotificationServiceController
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.trades.TradeSynchronizationHelper
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.i18n.i18n

class OpenTradesNotificationService(
    val notificationServiceController: NotificationServiceController,
    private val tradesServiceFacade: TradesServiceFacade): Logging {

    companion object {
        private const val STALE_TRADE_NOTIFICATION_THRESHOLD = 10 * 60 * 1000L // 10 minutes
    }

    fun launchNotificationService() {
        notificationServiceController.startService()
        runCatching {
            notificationServiceController.registerObserver(tradesServiceFacade.openTradeItems) { newValue ->
                log.d { "open trades in total: ${newValue.size}" }
                newValue.sortedByDescending { it.bisqEasyTradeModel.takeOfferDate }
                    .forEach { trade ->
                        onTradeUpdate(trade)
                    }

                // Check for trades that might have been completed while app was killed
                checkForMissedTradeCompletions(newValue)
            }
        }.onFailure {
            log.e(it) { "Failed to register observer" }
        }
    }

    fun stopNotificationService() {
        notificationServiceController.unregisterObserver(tradesServiceFacade.openTradeItems)
        // TODO unregister all ?
        notificationServiceController.stopService()
    }

    /**
     * Register to observe open trade state. Unregister when the trade concludes
     * Triggers push notifications
     */
    private fun onTradeUpdate(trade: TradeItemPresentationModel) {
        log.d { "open trade: $trade" }
        notificationServiceController.registerObserver(trade.bisqEasyTradeModel.tradeState) { newState ->
            log.d { "Open trade State Changed to: $newState" }

            if (OffersServiceFacade.isTerminalState(newState)) {
                // Trade is actually completed
                notificationServiceController.unregisterObserver(trade.bisqEasyTradeModel.tradeState)
                notificationServiceController.pushNotification(
                    "mobile.openTradeNotifications.tradeCompleted.title".i18n(trade.shortTradeId),
                    "mobile.openTradeNotifications.tradeCompleted.message".i18n(trade.peersUserName, newState)
                )
            } else if (shouldNotifyForTradeUpdate(newState)) {
                // Trade needs user attention but is not completed
                notificationServiceController.pushNotification(
                    "mobile.openTradeNotifications.needsAttention.title".i18n(trade.shortTradeId),
                    "mobile.openTradeNotifications.tradeUpdate.message".i18n(trade.peersUserName)
                )
            }
            // Don't notify for user's own actions or unimportant state changes
        }
    }

    /**
     * Determines if a trade state change should trigger an "update" notification.
     *
     * Uses a more permissive approach: notify for most state changes EXCEPT
     * obvious user-initiated actions that shouldn't generate notifications.
     */
    private fun shouldNotifyForTradeUpdate(state: BisqEasyTradeStateEnum): Boolean {
        val stateName = state.name
        return when {
            // Don't notify for user's own explicit actions
            state == BisqEasyTradeStateEnum.REJECTED -> false
            state == BisqEasyTradeStateEnum.CANCELLED -> false

            // Don't notify for initial states that don't represent progress
            state == BisqEasyTradeStateEnum.INIT -> false
            state == BisqEasyTradeStateEnum.TAKER_SENT_TAKE_OFFER_REQUEST -> false

            // For all other states, notify (this includes peer actions, received states, etc.)
            // This is more permissive and ensures we don't miss important trade progress
            else -> true
        }
    }

    /**
     * Checks for trades that might have been completed while the app was killed
     * and shows appropriate notifications.
     *
     * **Enhanced Logic**: Detects both completed trades and stale trades that need attention.
     * Shows completion notifications for trades that finished while the app was killed.
     *
     * **Purpose**: Ensures users get notified about trade completions even when the app
     * was not running when the trade completed.
     *
     * @param trades List of current trade items to check
     */
    private fun checkForMissedTradeCompletions(trades: List<TradeItemPresentationModel>) {
        try {
            log.d { "Checking for missed trade completions among ${trades.size} trades" }

            // Check for completed trades that might have finished while app was killed
            checkForCompletedTrades(trades)

            // Check for stale trades that need attention
            checkForStaleTrades(trades)

        } catch (e: Exception) {
            log.e(e) { "Error checking for missed trade completions" }
        }
    }

    /**
     * Checks for trades that completed while the app was killed and shows completion notifications.
     */
    private fun checkForCompletedTrades(trades: List<TradeItemPresentationModel>) {
        try {
            val completedTrades = trades.filter { trade ->
                val tradeState = trade.bisqEasyTradeModel.tradeState.value
                OffersServiceFacade.isTerminalState(tradeState)
            }

            if (completedTrades.isNotEmpty()) {
                log.i { "Found ${completedTrades.size} completed trades, showing completion notifications" }

                completedTrades.forEach { trade ->
                    val tradeState = trade.bisqEasyTradeModel.tradeState.value
                    log.i { "Showing completion notification for trade ${trade.shortTradeId} in state $tradeState" }

                    // Show completion notification
                    notificationServiceController.pushNotification(
                        "mobile.openTradeNotifications.tradeCompleted.title".i18n(trade.shortTradeId),
                        "mobile.openTradeNotifications.tradeCompleted.message".i18n(trade.peersUserName, tradeState.toString())
                    )
                }
            }
        } catch (e: Exception) {
            log.e(e) { "Error checking for completed trades" }
        }
    }

    /**
     * Checks for trades that have been stale for too long and shows attention notifications.
     */
    private fun checkForStaleTrades(trades: List<TradeItemPresentationModel>) {
        try {
            // Use shared synchronization helper to identify problematic trades
            val tradesNeedingAttention = TradeSynchronizationHelper.getTradesNeedingSync(trades)
                .filter { trade ->
                    val timeSinceCreation = kotlinx.datetime.Clock.System.now().toEpochMilliseconds() - trade.bisqEasyTradeModel.takeOfferDate
                    // Only notify for trades that have been open for more than 10 minutes
                    timeSinceCreation > STALE_TRADE_NOTIFICATION_THRESHOLD
                }

            if (tradesNeedingAttention.isNotEmpty()) {
                log.i { "Found ${tradesNeedingAttention.size} stale trades needing attention" }

                tradesNeedingAttention.forEach { trade ->
                    val tradeState = trade.bisqEasyTradeModel.tradeState.value
                    val timeSinceCreation = kotlinx.datetime.Clock.System.now().toEpochMilliseconds() - trade.bisqEasyTradeModel.takeOfferDate

                    log.i { "Trade ${trade.shortTradeId} needs attention - open for ${timeSinceCreation / 60000} minutes in state $tradeState" }

                    // Show a notification that the trade needs attention
                    notificationServiceController.pushNotification(
                        "mobile.openTradeNotifications.needsAttention.title".i18n(trade.shortTradeId),
                        "mobile.openTradeNotifications.staleState.message".i18n(trade.peersUserName, tradeState.toString())
                    )
                }
            }
        } catch (e: Exception) {
            log.e(e) { "Error checking for stale trades" }
        }
    }
}