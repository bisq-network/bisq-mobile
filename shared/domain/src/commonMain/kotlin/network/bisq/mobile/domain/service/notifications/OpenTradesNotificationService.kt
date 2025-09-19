package network.bisq.mobile.domain.service.notifications

import network.bisq.mobile.domain.data.replicated.chat.ChatMessageTypeEnum
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageModel
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.domain.service.notifications.controller.NotificationServiceController
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.i18n.i18n

/**
 * Service to manage notifications for open trades
 * Will update the user on important trade progress and new trades
 * whilst the bisq notification service is running (e.g. background app)
 */
class OpenTradesNotificationService(
    private val notificationServiceController: NotificationServiceController,
    private val tradesServiceFacade: TradesServiceFacade,
    private val userProfileServiceFacade: UserProfileServiceFacade,
) : Logging {

    private val observedTradeIds = mutableSetOf<String>()
    private val notifiedPaymentInfo = mutableSetOf<String>()
    private val notifiedBitcoinInfo = mutableSetOf<String>()
    private val perTradeFlows = mutableMapOf<String, List<kotlinx.coroutines.flow.StateFlow<*>>>()
    private val perTradePeerMessageCount = mutableMapOf<String, Int>()

    private fun getIgnoredProfileIds() = userProfileServiceFacade.ignoredProfileIds.value

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

        // Unregister per-trade observers to prevent leaks
        perTradeFlows.forEach { (_, tradeFlows) ->
            tradeFlows.forEach {
                notificationServiceController.unregisterObserver(it)
            }
        }
        perTradeFlows.clear()
        perTradePeerMessageCount.clear()

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
        val currentTradeIds =
            tradesServiceFacade.openTradeItems.value.map { it.shortTradeId }.toSet()

        val orphanedObserved = observedTradeIds - currentTradeIds
        val orphanedPayment = notifiedPaymentInfo - currentTradeIds
        val orphanedBitcoin = notifiedBitcoinInfo - currentTradeIds

        if (orphanedObserved.isNotEmpty() || orphanedPayment.isNotEmpty() || orphanedBitcoin.isNotEmpty()) {
            observedTradeIds.removeAll(orphanedObserved)
            notifiedPaymentInfo.removeAll(orphanedPayment)
            notifiedBitcoinInfo.removeAll(orphanedBitcoin)

            // Clean up orphaned per-trade flows
            orphanedObserved.forEach { tradeId ->
                perTradeFlows.remove(tradeId)?.let { flowList ->
                    flowList.forEach { notificationServiceController.unregisterObserver(it) }
                    perTradePeerMessageCount.remove(tradeId)
                }
            }

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
            observeChatMessages(trade)

            // Register the StateFlows for cleanup on service stop
            perTradeFlows[trade.shortTradeId] = listOf(
                trade.bisqEasyTradeModel.tradeState,
                trade.bisqEasyTradeModel.paymentAccountData,
                trade.bisqEasyTradeModel.bitcoinPaymentData,
                trade.bisqEasyOpenTradeChannelModel.chatMessages,
            )
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

    private fun observeChatMessages(trade: TradeItemPresentationModel) {
        perTradePeerMessageCount.put(
            trade.shortTradeId,
            getUnignoredMessageCount(trade.bisqEasyOpenTradeChannelModel.chatMessages.value),
        )
        notificationServiceController.registerObserver(trade.bisqEasyOpenTradeChannelModel.chatMessages) { newChatMessages ->
            log.d { "Chat messages updated for trade ${trade.shortTradeId}" }

            val currentPeerMsgCount =
                getUnignoredMessageCount(trade.bisqEasyOpenTradeChannelModel.chatMessages.value)
            val lastCount = perTradePeerMessageCount.getOrElse(trade.shortTradeId) { 0 }
            if (currentPeerMsgCount > lastCount) {
                // TODO: make pressing the notif open chat directly
                notificationServiceController.pushNotification(
                    "mobile.openTradeNotifications.newMessage.title".i18n(trade.shortTradeId),
                    "mobile.openTradeNotifications.newMessage.message".i18n(trade.peersUserName)
                )
            }
            perTradePeerMessageCount.put(
                trade.shortTradeId,
                currentPeerMsgCount,
            )
        }
    }

    private fun getUnignoredMessageCount(chatMessages: Set<BisqEasyOpenTradeMessageModel>): Int {
        val ignoredIds = getIgnoredProfileIds()
        return chatMessages.filter {
            it.chatMessageType == ChatMessageTypeEnum.TEXT && !it.isMyMessage && it.senderUserProfileId !in ignoredIds
        }.size
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
                notificationServiceController.unregisterObserver(trade.bisqEasyOpenTradeChannelModel.chatMessages)
                observedTradeIds.remove(trade.shortTradeId)
                notifiedPaymentInfo.remove(trade.shortTradeId)
                notifiedBitcoinInfo.remove(trade.shortTradeId)
                perTradeFlows.remove(trade.shortTradeId)
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
                    "mobile.openTradeNotifications.youSentFiat.title" to "mobile.openTradeNotifications.youSentFiat.message"
                } else {
                    // User is seller -> peer (buyer) confirmed sending payment
                    "mobile.openTradeNotifications.peerSentFiat.title" to "mobile.openTradeNotifications.peerSentFiat.message"
                }

                notificationServiceController.pushNotification(
                    titleKey.i18n(trade.shortTradeId),
                    messageKey.i18n(trade.peersUserName)
                )
            }
            BisqEasyTradeStateEnum.SELLER_RECEIVED_FIAT_SENT_CONFIRMATION -> {
                val (titleKey, messageKey) = if (trade.bisqEasyTradeModel.isSeller) {
                    // User is seller -> they received confirmation that buyer sent payment
                    "mobile.openTradeNotifications.youReceivedFiatConfirmation.title" to "mobile.openTradeNotifications.youReceivedFiatConfirmation.message"
                } else {
                    // User is buyer -> seller received their payment confirmation (from buyer's perspective, they sent it)
                    "mobile.openTradeNotifications.youSentFiat.title" to "mobile.openTradeNotifications.youSentFiat.message"
                }

                notificationServiceController.pushNotification(
                    titleKey.i18n(trade.shortTradeId),
                    messageKey.i18n(trade.peersUserName)
                )
            }
            BisqEasyTradeStateEnum.SELLER_CONFIRMED_FIAT_RECEIPT -> {
                val (titleKey, messageKey) = if (trade.bisqEasyTradeModel.isBuyer) {
                    // User is buyer -> peer (seller) confirmed receiving the payment
                    "mobile.openTradeNotifications.peerReceivedFiat.title" to "mobile.openTradeNotifications.peerReceivedFiat.message"
                } else {
                    // User is seller -> they confirmed receiving payment
                    "mobile.openTradeNotifications.youReceivedFiat.title" to "mobile.openTradeNotifications.youReceivedFiat.message"
                }

                notificationServiceController.pushNotification(
                    titleKey.i18n(trade.shortTradeId),
                    messageKey.i18n(trade.peersUserName)
                )
            }
            BisqEasyTradeStateEnum.SELLER_SENT_BTC_SENT_CONFIRMATION -> {
                val (titleKey, messageKey) = if (trade.bisqEasyTradeModel.isSeller) {
                    // User is seller -> they confirmed sending Bitcoin
                    "mobile.openTradeNotifications.youSentBtc.title" to "mobile.openTradeNotifications.youSentBtc.message"
                } else {
                    // User is buyer -> peer (seller) confirmed sending Bitcoin
                    "mobile.openTradeNotifications.peerSentBtc.title" to "mobile.openTradeNotifications.peerSentBtc.message"
                }

                notificationServiceController.pushNotification(
                    titleKey.i18n(trade.shortTradeId),
                    messageKey.i18n(trade.peersUserName)
                )
            }
            BisqEasyTradeStateEnum.BUYER_RECEIVED_BTC_SENT_CONFIRMATION -> {
                val (titleKey, messageKey) = if (trade.bisqEasyTradeModel.isBuyer) {
                    // User is buyer -> they received confirmation that seller sent Bitcoin
                    "mobile.openTradeNotifications.youReceivedBtc.title" to "mobile.openTradeNotifications.youReceivedBtc.message"
                } else {
                    // User is seller -> buyer received their Bitcoin confirmation (from seller's perspective, they sent it)
                    "mobile.openTradeNotifications.youSentBtc.title" to "mobile.openTradeNotifications.youSentBtc.message"
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