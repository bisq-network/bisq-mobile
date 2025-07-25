package network.bisq.mobile.domain.service.notifications

import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.service.notifications.controller.NotificationServiceController
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.trades.TradeSynchronizationHelper
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.i18n.i18n

class OpenTradesNotificationService(
    val notificationServiceController: NotificationServiceController,
    private val tradesServiceFacade: TradesServiceFacade): Logging {

    fun launchNotificationService() {
        notificationServiceController.startService()
        runCatching {
            notificationServiceController.registerObserver(tradesServiceFacade.openTradeItems) { newValue ->
                log.d { "KMP: open trades in total: ${newValue.size}" }
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
        notificationServiceController.registerObserver(trade.bisqEasyTradeModel.tradeState) {
            log.d { "Open trade State Changed to: $it" }
            if (OffersServiceFacade.isTerminalState(it)) {
                notificationServiceController.unregisterObserver(trade.bisqEasyTradeModel.tradeState)
                notificationServiceController.pushNotification(
                    "mobile.openTradeNotifications.tradeCompleted.title".i18n(trade.shortTradeId),
                    "mobile.openTradeNotifications.tradeCompleted.message".i18n(trade.peersUserName, it)
                )
            } else {
                notificationServiceController.pushNotification(
                    "mobile.openTradeNotifications.needsAttention.title".i18n(trade.shortTradeId),
                    "mobile.openTradeNotifications.needsAttention.message".i18n(trade.peersUserName)
                )
            }

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
            log.d { "KMP: Checking for missed trade completions among ${trades.size} trades" }

            // Check for completed trades that might have finished while app was killed
            checkForCompletedTrades(trades)

            // Check for stale trades that need attention
            checkForStaleTrades(trades)

        } catch (e: Exception) {
            log.e(e) { "KMP: Error checking for missed trade completions" }
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
                log.i { "KMP: Found ${completedTrades.size} completed trades, showing completion notifications" }

                completedTrades.forEach { trade ->
                    val tradeState = trade.bisqEasyTradeModel.tradeState.value
                    log.i { "KMP: Showing completion notification for trade ${trade.shortTradeId} in state $tradeState" }

                    // Show completion notification
                    notificationServiceController.pushNotification(
                        "mobile.openTradeNotifications.tradeCompleted.title".i18n(trade.shortTradeId),
                        "mobile.openTradeNotifications.tradeCompleted.message".i18n(trade.peersUserName, tradeState.toString())
                    )
                }
            }
        } catch (e: Exception) {
            log.e(e) { "KMP: Error checking for completed trades" }
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
                    val timeSinceCreation = System.currentTimeMillis() - trade.bisqEasyTradeModel.takeOfferDate
                    // Only notify for trades that have been open for more than 10 minutes
                    timeSinceCreation > 10 * 60 * 1000
                }

            if (tradesNeedingAttention.isNotEmpty()) {
                log.i { "KMP: Found ${tradesNeedingAttention.size} stale trades needing attention" }

                tradesNeedingAttention.forEach { trade ->
                    val tradeState = trade.bisqEasyTradeModel.tradeState.value
                    val timeSinceCreation = System.currentTimeMillis() - trade.bisqEasyTradeModel.takeOfferDate

                    log.i { "KMP: Trade ${trade.shortTradeId} needs attention - open for ${timeSinceCreation / 60000} minutes in state $tradeState" }

                    // Show a notification that the trade needs attention
                    notificationServiceController.pushNotification(
                        "mobile.openTradeNotifications.needsAttention.title".i18n(trade.shortTradeId),
                        "mobile.openTradeNotifications.staleState.message".i18n(trade.peersUserName, tradeState.toString())
                    )
                }
            }
        } catch (e: Exception) {
            log.e(e) { "KMP: Error checking for stale trades" }
        }
    }
}