package network.bisq.mobile.domain.service.notifications

import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.domain.service.notifications.controller.NotificationServiceController
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.i18n.i18n

/**
 * Service to manage notifications for open trades
 * Will update the user on important trade progress and new trades
 * whilst the bisq notification service is running (e.g. background app)
 */
class OpenTradesNotificationService(
    val notificationServiceController: NotificationServiceController,
    private val tradesServiceFacade: TradesServiceFacade): Logging {

    private val observedTradeIds = mutableSetOf<String>()
    private val notifiedPaymentInfo = mutableSetOf<String>()
    private val notifiedBitcoinInfo = mutableSetOf<String>()

    fun launchNotificationService() {
        notificationServiceController.startService()
        runCatching {
            notificationServiceController.registerObserver(tradesServiceFacade.openTradeItems) { newValue ->
                log.d { "open trades in total: ${newValue.size}" }
                cleanupOrphanedTrades()
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
        notificationServiceController.stopService()

        // Clear all tracking sets to prevent memory leaks
        observedTradeIds.clear()
        notifiedPaymentInfo.clear()
        notifiedBitcoinInfo.clear()
        log.d { "OpenTradesNotificationService stopped and all tracking sets cleared" }
    }

    /**
     * Clean up orphaned trade IDs that are no longer in the active trades list.
     * This prevents memory leaks from trades that were removed from the system.
     */
    private fun cleanupOrphanedTrades() {
        val currentTradeIds = tradesServiceFacade.openTradeItems.value.map { it.shortTradeId }.toSet()

        val orphanedObserved = observedTradeIds - currentTradeIds
        val orphanedPayment = notifiedPaymentInfo - currentTradeIds
        val orphanedBitcoin = notifiedBitcoinInfo - currentTradeIds

        if (orphanedObserved.isNotEmpty() || orphanedPayment.isNotEmpty() || orphanedBitcoin.isNotEmpty()) {
            observedTradeIds.removeAll(orphanedObserved)
            notifiedPaymentInfo.removeAll(orphanedPayment)
            notifiedBitcoinInfo.removeAll(orphanedBitcoin)

            log.d { "Cleaned up orphaned trades - observed: $orphanedObserved, payment: $orphanedPayment, bitcoin: $orphanedBitcoin" }
        }
    }

    /**
     * Register to observe open trade state. Unregister when the trade concludes
     * Triggers push notifications for important trade state changes
     */
    private fun onTradeUpdate(trade: TradeItemPresentationModel) {
        log.d { "onTradeUpdate called for trade ${trade.shortTradeId}" }

        // First, check the current state and send notification if needed
        val currentState = trade.bisqEasyTradeModel.tradeState.value
        log.d { "Current trade state for ${trade.shortTradeId}: $currentState" }
        handleTradeStateNotification(trade, currentState, isInitialState = true)

        // Then all observers:
        if (observedTradeIds.add(trade.shortTradeId)) {
            observeFutureStateChanges(trade)
            observePaymentAccountData(trade)
            observeBitcoinPaymentData(trade)
        } else {
            log.d { "Observers already registered for trade ${trade.shortTradeId}" }
        }
    }

    private fun observeBitcoinPaymentData(trade: TradeItemPresentationModel) {
        notificationServiceController.registerObserver(trade.bisqEasyTradeModel.bitcoinPaymentData) { bitcoinData ->
            log.d { "Bitcoin payment data changed for trade ${trade.shortTradeId}: ${bitcoinData?.isNotEmpty()}" }
            if (!bitcoinData.isNullOrEmpty() && notifiedBitcoinInfo.add(trade.shortTradeId)) {
                // Determine if user sent or received bitcoin info based on trade role
                val (titleKey, messageKey) = if (trade.bisqEasyTradeModel.isBuyer) {
                    // User is buyer -> they sent bitcoin info
                    "mobile.openTradeNotifications.bitcoinInfoSent.title" to "mobile.openTradeNotifications.bitcoinInfoSent.message"
                } else {
                    // User is seller -> they received bitcoin info
                    "mobile.openTradeNotifications.bitcoinInfoReceived.title" to "mobile.openTradeNotifications.bitcoinInfoReceived.message"
                }

                notificationServiceController.pushNotification(
                    titleKey.i18n(trade.shortTradeId),
                    messageKey.i18n(trade.peersUserName)
                )
            }
        }
    }

    private fun observePaymentAccountData(trade: TradeItemPresentationModel) {
        notificationServiceController.registerObserver(trade.bisqEasyTradeModel.paymentAccountData) { paymentData ->
            log.d { "Payment account data changed for trade ${trade.shortTradeId}: ${paymentData?.isNotEmpty()}" }
            if (!paymentData.isNullOrEmpty() && notifiedPaymentInfo.add(trade.shortTradeId)) {
                // Determine if user sent or received payment info based on trade role
                val (titleKey, messageKey) = if (trade.bisqEasyTradeModel.isSeller) {
                    // User is seller -> they sent payment info
                    "mobile.openTradeNotifications.paymentInfoSent.title" to "mobile.openTradeNotifications.paymentInfoSent.message"
                } else {
                    // User is buyer -> they received payment info
                    "mobile.openTradeNotifications.paymentInfoReceived.title" to "mobile.openTradeNotifications.paymentInfoReceived.message"
                }

                notificationServiceController.pushNotification(
                    titleKey.i18n(trade.shortTradeId),
                    messageKey.i18n(trade.peersUserName)
                )
            }
        }
    }

    private fun observeFutureStateChanges(trade: TradeItemPresentationModel) {
        notificationServiceController.registerObserver(trade.bisqEasyTradeModel.tradeState) { newState ->
            log.d { "Trade State Changed to: $newState for trade ${trade.shortTradeId}" }
            handleTradeStateNotification(trade, newState, isInitialState = false)

            // Unregister observer when trade concludes
            if (OffersServiceFacade.isTerminalState(newState)) {
                notificationServiceController.unregisterObserver(trade.bisqEasyTradeModel.tradeState)
                notificationServiceController.unregisterObserver(trade.bisqEasyTradeModel.paymentAccountData)
                notificationServiceController.unregisterObserver(trade.bisqEasyTradeModel.bitcoinPaymentData)
                observedTradeIds.remove(trade.shortTradeId)
                notifiedPaymentInfo.remove(trade.shortTradeId)
                notifiedBitcoinInfo.remove(trade.shortTradeId)
                log.d { "Trade ${trade.shortTradeId} completed and unregistered for notification updates" }
            }
        }
    }

    /**
     * Handle trade state notifications for both initial states and state changes
     */
    private fun handleTradeStateNotification(trade: TradeItemPresentationModel, state: BisqEasyTradeStateEnum, isInitialState: Boolean) {
        log.d { "handleTradeStateNotification - trade: ${trade.shortTradeId}, state: $state, isInitial: $isInitialState" }

        // Send notifications for important intermediate states
        when (state) {
            // Payment related states
            BisqEasyTradeStateEnum.BUYER_SENT_FIAT_SENT_CONFIRMATION -> {
                val (titleKey, messageKey) = if (trade.bisqEasyTradeModel.isBuyer) {
                    // User is buyer -> they confirmed sending payment
                    "mobile.openTradeNotifications.fiatSent.title" to "mobile.openTradeNotifications.fiatSent.message"
                } else {
                    // User is seller -> they received confirmation that buyer sent payment
                    "mobile.openTradeNotifications.fiatSentReceived.title" to "mobile.openTradeNotifications.fiatSentReceived.message"
                }

                notificationServiceController.pushNotification(
                    titleKey.i18n(trade.shortTradeId),
                    messageKey.i18n(trade.peersUserName)
                )
            }
            BisqEasyTradeStateEnum.SELLER_RECEIVED_FIAT_SENT_CONFIRMATION -> {
                val (titleKey, messageKey) = if (trade.bisqEasyTradeModel.isSeller) {
                    // User is seller -> they received confirmation that buyer sent payment
                    "mobile.openTradeNotifications.fiatSentReceived.title" to "mobile.openTradeNotifications.fiatSentReceived.message"
                } else {
                    // User is buyer -> seller received their payment confirmation
                    "mobile.openTradeNotifications.fiatSent.title" to "mobile.openTradeNotifications.fiatSent.message"
                }

                notificationServiceController.pushNotification(
                    titleKey.i18n(trade.shortTradeId),
                    messageKey.i18n(trade.peersUserName)
                )
            }
            BisqEasyTradeStateEnum.SELLER_CONFIRMED_FIAT_RECEIPT -> {
                val (titleKey, messageKey) = if (trade.bisqEasyTradeModel.isBuyer) {
                    // User is buyer -> they confirmed sending payment
                    "mobile.openTradeNotifications.fiatSent.title" to "mobile.openTradeNotifications.fiatSent.message"
                } else {
                    // User is seller -> they confirmed receiving payment
                    "mobile.openTradeNotifications.fiatReceived.title" to "mobile.openTradeNotifications.fiatReceived.message"
                }

                notificationServiceController.pushNotification(
                    titleKey.i18n(trade.shortTradeId),
                    messageKey.i18n(trade.peersUserName)
                )
            }
            BisqEasyTradeStateEnum.SELLER_SENT_BTC_SENT_CONFIRMATION -> {
                val (titleKey, messageKey) = if (trade.bisqEasyTradeModel.isSeller) {
                    // User is seller -> they confirmed sending Bitcoin
                    "mobile.openTradeNotifications.btcSent.title" to "mobile.openTradeNotifications.btcSent.message"
                } else {
                    // User is buyer -> they received confirmation that seller sent Bitcoin
                    "mobile.openTradeNotifications.btcSentReceived.title" to "mobile.openTradeNotifications.btcSentReceived.message"
                }

                notificationServiceController.pushNotification(
                    titleKey.i18n(trade.shortTradeId),
                    messageKey.i18n(trade.peersUserName)
                )
            }
            BisqEasyTradeStateEnum.BUYER_RECEIVED_BTC_SENT_CONFIRMATION -> {
                val (titleKey, messageKey) = if (trade.bisqEasyTradeModel.isBuyer) {
                    // User is buyer -> they received confirmation that seller sent Bitcoin
                    "mobile.openTradeNotifications.btcSentReceived.title" to "mobile.openTradeNotifications.btcSentReceived.message"
                } else {
                    // User is seller -> buyer received their Bitcoin confirmation
                    "mobile.openTradeNotifications.btcSent.title" to "mobile.openTradeNotifications.btcSent.message"
                }

                notificationServiceController.pushNotification(
                    titleKey.i18n(trade.shortTradeId),
                    messageKey.i18n(trade.peersUserName)
                )
            }

            // Early trade states that might be missed - offer taking notifications
            BisqEasyTradeStateEnum.TAKER_SENT_TAKE_OFFER_REQUEST -> {
                if (!isInitialState) { // Only notify on state changes, not initial discovery
                    notificationServiceController.pushNotification(
                        "mobile.openTradeNotifications.offerTaken.title".i18n(trade.shortTradeId),
                        "mobile.openTradeNotifications.offerTaken.message".i18n(trade.peersUserName)
                    )
                }
            }

            // Maker states - when someone takes the user's offer (user is maker)
            BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
            BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA -> {
                if (!isInitialState) { // Only notify on state changes, not initial discovery
                    notificationServiceController.pushNotification(
                        "mobile.openTradeNotifications.offerTaken.title".i18n(trade.shortTradeId),
                        "mobile.openTradeNotifications.offerTaken.message".i18n(trade.peersUserName)
                    )
                }
            }

            // States where payment account info is exchanged
            BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
            BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS_,
            BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA -> {
                if (!isInitialState) { // Only notify on state changes, not initial discovery
                    // Determine if user sent or received payment info based on trade role
                    val (titleKey, messageKey) = if (trade.bisqEasyTradeModel.isSeller) {
                        // User is seller -> they sent payment info
                        "mobile.openTradeNotifications.paymentInfoSent.title" to "mobile.openTradeNotifications.paymentInfoSent.message"
                    } else {
                        // User is buyer -> they received payment info
                        "mobile.openTradeNotifications.paymentInfoReceived.title" to "mobile.openTradeNotifications.paymentInfoReceived.message"
                    }

                    notificationServiceController.pushNotification(
                        titleKey.i18n(trade.shortTradeId),
                        messageKey.i18n(trade.peersUserName)
                    )
                }
            }
            else -> {
                if (OffersServiceFacade.isTerminalState(state)) {
                    val translatedState = translatedI18N(state)
                    notificationServiceController.pushNotification(
                        "mobile.openTradeNotifications.tradeCompleted.title".i18n(trade.shortTradeId),
                        "mobile.openTradeNotifications.tradeCompleted.message".i18n(trade.peersUserName, translatedState)
                    )
                }
            }
        }
    }

    private fun translatedI18N(state: BisqEasyTradeStateEnum): String {
        return when (state) {
            BisqEasyTradeStateEnum.BTC_CONFIRMED -> "mobile.tradeState.completed".i18n()
            BisqEasyTradeStateEnum.REJECTED -> "mobile.tradeState.rejected".i18n()
            BisqEasyTradeStateEnum.PEER_REJECTED -> "mobile.tradeState.peerRejected".i18n()
            BisqEasyTradeStateEnum.CANCELLED -> "mobile.tradeState.cancelled".i18n()
            BisqEasyTradeStateEnum.PEER_CANCELLED -> "mobile.tradeState.peerCancelled".i18n()
            BisqEasyTradeStateEnum.FAILED -> "mobile.tradeState.failed".i18n()
            BisqEasyTradeStateEnum.FAILED_AT_PEER -> "mobile.tradeState.failedAtPeer".i18n()
            else -> state.toString() // Fallback to raw state if no translation available
        }.replaceFirstChar { it.titlecase() }
    }
}