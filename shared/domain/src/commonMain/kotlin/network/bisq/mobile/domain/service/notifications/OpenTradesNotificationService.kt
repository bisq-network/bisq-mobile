package network.bisq.mobile.domain.service.notifications

import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.service.notifications.controller.NotificationServiceController
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
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
     * **Purpose**: Detects trades that have been open for an unusually long time in
     * intermediate states, which may indicate missed completion messages.
     *
     * **Logic**: If a trade has been open for more than 30 minutes and is still in
     * a non-terminal state, it shows a notification that the trade needs attention.
     *
     * **User Experience**: Provides proactive notifications to users about trades
     * that may require manual intervention or have missed state updates.
     *
     * @param trades List of current trade items to check
     */
    private fun checkForMissedTradeCompletions(trades: List<TradeItemPresentationModel>) {
        try {
            log.d { "KMP: Checking for missed trade completions among ${trades.size} trades" }

            trades.forEach { trade ->
                val tradeState = trade.bisqEasyTradeModel.tradeState.value
                val timeSinceCreation = System.currentTimeMillis() - trade.bisqEasyTradeModel.takeOfferDate

                // If a trade has been open for more than 30 minutes and is still in an intermediate state,
                // it might have missed completion messages
                if (timeSinceCreation > 30 * 60 * 1000 && !OffersServiceFacade.isTerminalState(tradeState)) {
                    log.i { "KMP: Trade ${trade.shortTradeId} has been open for ${timeSinceCreation / 60000} minutes in state $tradeState" }

                    // Show a notification that the trade needs attention
                    notificationServiceController.pushNotification(
                        "mobile.openTradeNotifications.needsAttention.title".i18n(trade.shortTradeId),
                        "mobile.openTradeNotifications.staleState.message".i18n(trade.peersUserName, tradeState.toString())
                    )
                }
            }
        } catch (e: Exception) {
            log.e(e) { "KMP: Error checking for missed trade completions" }
        }
    }
}