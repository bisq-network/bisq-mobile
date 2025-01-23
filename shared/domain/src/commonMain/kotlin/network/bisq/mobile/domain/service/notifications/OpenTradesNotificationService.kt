package network.bisq.mobile.domain.service.notifications

import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.service.notifications.controller.NotificationServiceController
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.utils.Logging

class OpenTradesNotificationService(
    val notificationServiceController: NotificationServiceController,
    private val tradesServiceFacade: TradesServiceFacade): Logging {

    fun launchNotificationService() {
        notificationServiceController.startService()
        runCatching {
            notificationServiceController.registerObserver(tradesServiceFacade.openTradeItems) { newValue ->
                log.d { "open trades in total: ${newValue.size}" }
                newValue.sortedByDescending { it.bisqEasyTradeModel.takeOfferDate }
                    .forEach { trade ->
                        onTradeUpdate(trade)
                    }
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
                    "Trade [${trade.shortTradeId}] completed",
                    "Your trade with ${trade.peersUserName} has finished as ${it}"
                )
            } else {
                notificationServiceController.pushNotification(
                    "Trade [${trade.shortTradeId}] activity",
                    "Your trade with ${trade.peersUserName} needs your attention"
                )
            }

        }
    }
}