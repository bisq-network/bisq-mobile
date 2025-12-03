package network.bisq.mobile.presentation.common.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.Clock
import network.bisq.mobile.domain.data.replicated.chat.ChatMessageTypeEnum
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageModel
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.domain.service.ForegroundDetector
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.notification.ForegroundServiceController
import network.bisq.mobile.presentation.common.notification.NotificationChannels
import network.bisq.mobile.presentation.common.notification.NotificationController
import network.bisq.mobile.presentation.common.notification.NotificationIds
import network.bisq.mobile.presentation.common.notification.model.android.AndroidNotificationCategory
import network.bisq.mobile.presentation.common.notification.model.NotificationPressAction
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute

/**
 * Service to manage notifications for open trades
 * Will update the user on important trade progress and new trades
 * whilst the bisq notification service is running (e.g. background app)
 */
class OpenTradesNotificationService(
    private val notificationController: NotificationController,
    private val foregroundServiceController: ForegroundServiceController,
    private val tradesServiceFacade: TradesServiceFacade,
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val appForegroundController: ForegroundDetector
) : Logging {

    private val observedTradeIds = mutableSetOf<String>()

    // Payment account info can trigger notifications via both trade state transitions and data changes
    // This set prevents duplicate notifications for the same trade
    private val notifiedPaymentInfo = mutableSetOf<String>()
    private val perTradeFlows = mutableMapOf<String, MutableList<Flow<*>>>()
    private val perTradePeerMessageCount = mutableMapOf<String, Int>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var lifecycleObserverJob: Job? = null

    companion object {
        private const val FOREGROUND_DEBOUNCE_MS = 1000L
    }

    init {
        setupLifecycleObserver()
    }

    @OptIn(FlowPreview::class)
    private fun setupLifecycleObserver() {
        if (lifecycleObserverJob?.isActive == true) {
            log.d { "Lifecycle observer is already running." }
            return
        }

        lifecycleObserverJob = appForegroundController.isForeground
            .debounce(FOREGROUND_DEBOUNCE_MS)
            .distinctUntilChanged()
            .onEach { isForeground ->
                if (isForeground) {
                    log.d { "App entered foreground (debounced). Stopping service and observers." }
                    stopObserversAndService()
                } else {
                    log.d { "App entered background (debounced). Starting service and observers." }
                    startServiceAndObservers()
                }
            }
            .launchIn(scope)
    }

    private fun getIgnoredProfileIds() = userProfileServiceFacade.ignoredProfileIds.value

    fun stopNotificationService() {
        log.d { "Permanently stopping OpenTradesNotificationService." }
        lifecycleObserverJob?.cancel()
        lifecycleObserverJob = null
        stopObserversAndService()
        foregroundServiceController.dispose()
        scope.cancel()
    }

    private fun startServiceAndObservers() {
        foregroundServiceController.startService()
        runCatching {
            foregroundServiceController.registerObserver(tradesServiceFacade.openTradeItems) { trades ->
                log.d { "open trades in total: ${trades.size}" }
                cleanupOrphanedTrades()
                trades.sortedByDescending { it.bisqEasyTradeModel.takeOfferDate }
                    .forEach { trade -> onTradeUpdate(trade) }
            }
        }.onFailure {
            log.e(it) { "Failed to register observer for openTradeItems" }
        }
    }

    private fun stopObserversAndService() {
        foregroundServiceController.unregisterObservers()
        perTradeFlows.clear()
        perTradePeerMessageCount.clear()

        foregroundServiceController.stopService()

        observedTradeIds.clear()
        notifiedPaymentInfo.clear()
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

        if (orphanedObserved.isNotEmpty()) {
            notifiedPaymentInfo.removeAll(orphanedPayment)
            observedTradeIds.removeAll(orphanedObserved)

            // Clean up orphaned per-trade flows
            orphanedObserved.forEach { tradeId ->
                perTradeFlows.remove(tradeId)?.let { flowList ->
                    flowList.forEach { foregroundServiceController.unregisterObserver(it) }
                    perTradePeerMessageCount.remove(tradeId)
                }
            }

            log.d { "Cleaned up orphaned trades - observed: $orphanedObserved" }
        }
    }

    /**
     * Check if the trade was taken within the last 10 seconds
     */
    private fun isTradeRecentlyTaken(trade: TradeItemPresentationModel): Boolean {
        return try {
            val takeOfferDate = trade.bisqEasyTradeModel.takeOfferDate
            val currentTime = Clock.System.now().toEpochMilliseconds()
            val timeDifference = currentTime - takeOfferDate
            val isRecent = timeDifference < 10_000 // 10 seconds in milliseconds

            log.d {
                "Trade ${trade.shortTradeId} taken at $takeOfferDate (formatted: ${trade.formattedDate} ${trade.formattedTime}), " +
                        "current time: $currentTime, difference: ${timeDifference}ms, isRecent: $isRecent"
            }

            isRecent
        } catch (e: Exception) {
            log.e(e) { "Error checking if trade ${trade.shortTradeId} is recently taken" }
            false
        }
    }

    /**
     * Register to observe open trade state. Unregister when the trade concludes
     * Triggers push notifications for important trade state changes
     */
    private fun onTradeUpdate(trade: TradeItemPresentationModel) {
        val currentState = trade.bisqEasyTradeModel.tradeState.value
        log.d { "onTradeUpdate called for trade ${trade.shortTradeId}: $currentState" }

        // Only trigger initial state notification if trade was taken within last 10 seconds
        if (isTradeRecentlyTaken(trade)) {
            log.d { "Trade ${trade.shortTradeId} is recent (within 10s), triggering initial state notification" }
            handleTradeStateNotification(trade, currentState)
        } else {
            log.d { "Trade ${trade.shortTradeId} is not recent, skipping initial state notification" }
        }

        // Register observers for this trade if not already done
        if (observedTradeIds.add(trade.shortTradeId)) {
            observeFutureStateChanges(trade)
            observePaymentAccountData(trade)
            observeBitcoinPaymentData(trade)
            observeChatMessages(trade)
        } else {
            log.d { "Observers already registered for trade ${trade.shortTradeId}" }
        }
    }

    /**
     * Helper function to register a flow observer for a specific trade
     * Skips initial value and only emits on actual state changes
     */
    private fun <T> registerTradeFlowObserver(
        trade: TradeItemPresentationModel,
        flow: Flow<T>,
        onStateChange: (T) -> Unit
    ) {
        val changeFlow = flow
            .distinctUntilChanged()  // Only emit when state actually changes
            .drop(1)  // Skip the initial/current value

        perTradeFlows.getOrPut(trade.shortTradeId) { mutableListOf() }.add(changeFlow)
        foregroundServiceController.registerObserver(changeFlow, onStateChange)
    }

    private fun observeFutureStateChanges(trade: TradeItemPresentationModel) {
        // Register observer for trade state changes
        registerTradeFlowObserver(trade, trade.bisqEasyTradeModel.tradeState) { newState ->
            log.d { "Trade State Changed to: $newState for trade ${trade.shortTradeId}" }
            handleTradeStateNotification(trade, newState)

            // Clean up when trade concludes
            if (OffersServiceFacade.Companion.isTerminalState(newState)) {
                observedTradeIds.remove(trade.shortTradeId)
                perTradePeerMessageCount.remove(trade.shortTradeId)
                notifiedPaymentInfo.remove(trade.shortTradeId)

                // Unregister the flows for this trade
                perTradeFlows.remove(trade.shortTradeId)?.forEach { flow ->
                    foregroundServiceController.unregisterObserver(flow)
                }
                log.d { "Trade ${trade.shortTradeId} completed and unregistered for notification updates" }
            }
        }
    }

    private fun observeBitcoinPaymentData(trade: TradeItemPresentationModel) {
        // Register observer for bitcoin payment data changes
        registerTradeFlowObserver(
            trade,
            trade.bisqEasyTradeModel.bitcoinPaymentData
        ) { bitcoinData ->
            log.d { "Bitcoin payment data changed for trade ${trade.shortTradeId}: ${bitcoinData?.isNotEmpty()}" }
            // Determine if user sent or received bitcoin info based on trade role
            val (titleKey, messageKey) = if (trade.bisqEasyTradeModel.isBuyer) {
                // User is buyer -> they sent bitcoin info
                "mobile.openTradeNotifications.bitcoinInfoSent.title" to "mobile.openTradeNotifications.bitcoinInfoSent.message"
            } else {
                // User is seller -> they received bitcoin info
                "mobile.openTradeNotifications.bitcoinInfoReceived.title" to "mobile.openTradeNotifications.bitcoinInfoReceived.message"
            }

            notify(
                trade,
                NotificationIds.getBitcoinPaymentUpdatedId(trade.shortTradeId),
                titleKey.i18n(trade.shortTradeId),
                messageKey.i18n(trade.peersUserName),
            )
        }
    }

    private fun observePaymentAccountData(trade: TradeItemPresentationModel) {
        registerTradeFlowObserver(
            trade,
            trade.bisqEasyTradeModel.paymentAccountData
        ) { paymentData ->
            if (notifiedPaymentInfo.add(trade.shortTradeId)) {
                log.d { "Payment account data changed for trade ${trade.shortTradeId}: ${paymentData?.isNotEmpty()}" }
                // Determine if user sent or received payment info based on trade role
                val (titleKey, messageKey) = if (trade.bisqEasyTradeModel.isSeller) {
                    // User is seller -> they sent payment info
                    "mobile.openTradeNotifications.paymentInfoSent.title" to "mobile.openTradeNotifications.paymentInfoSent.message"
                } else {
                    // User is buyer -> they received payment info
                    "mobile.openTradeNotifications.paymentInfoReceived.title" to "mobile.openTradeNotifications.paymentInfoReceived.message"
                }

                notify(
                    trade,
                    NotificationIds.getPaymentUpdatedId(trade.shortTradeId),
                    titleKey.i18n(trade.shortTradeId),
                    messageKey.i18n(trade.peersUserName),
                )
            }
        }
    }

    private fun observeChatMessages(trade: TradeItemPresentationModel) {
        // Initialize chat message count
        perTradePeerMessageCount[trade.shortTradeId] =
            getUnignoredMessageCount(trade.bisqEasyOpenTradeChannelModel.chatMessages.value)

        // Register observer for chat message changes
        registerTradeFlowObserver(
            trade,
            trade.bisqEasyOpenTradeChannelModel.chatMessages
        ) { newChatMessages ->
            log.d { "Chat messages updated for trade ${trade.shortTradeId}" }
            val currentPeerMsgCount = getUnignoredMessageCount(newChatMessages)
            val lastCount = perTradePeerMessageCount[trade.shortTradeId] ?: 0
            if (currentPeerMsgCount > lastCount) {
                notify(
                    trade,
                    NotificationIds.getNewChatMessageId(trade.shortTradeId),
                    "mobile.openTradeNotifications.newMessage.title".i18n(trade.shortTradeId),
                    "mobile.openTradeNotifications.newMessage.message".i18n(trade.peersUserName),
                    true,
                )
            }
            perTradePeerMessageCount[trade.shortTradeId] = currentPeerMsgCount
        }
    }

    private fun getUnignoredMessageCount(chatMessages: Set<BisqEasyOpenTradeMessageModel>): Int {
        val ignoredIds = getIgnoredProfileIds()
        return chatMessages.filter {
            it.chatMessageType == ChatMessageTypeEnum.TEXT && !it.isMyMessage && it.senderUserProfileId !in ignoredIds
        }.size
    }

    /**
     * Handle trade state notifications for both initial states and state changes
     */
    private fun handleTradeStateNotification(
        trade: TradeItemPresentationModel,
        state: BisqEasyTradeStateEnum
    ) {
        log.d { "handleTradeStateNotification - trade: ${trade.shortTradeId}, state: $state" }

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

                notify(
                    trade,
                    NotificationIds.getTradeStateUpdatedId(trade.shortTradeId),
                    titleKey.i18n(trade.shortTradeId),
                    messageKey.i18n(trade.peersUserName),
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

                notify(
                    trade,
                    NotificationIds.getTradeStateUpdatedId(trade.shortTradeId),
                    titleKey.i18n(trade.shortTradeId),
                    messageKey.i18n(trade.peersUserName),
                )
            }

            BisqEasyTradeStateEnum.BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION,
            BisqEasyTradeStateEnum.SELLER_CONFIRMED_FIAT_RECEIPT -> {
                val (titleKey, messageKey) = if (trade.bisqEasyTradeModel.isBuyer) {
                    // User is buyer -> peer (seller) confirmed receiving the payment
                    "mobile.openTradeNotifications.peerReceivedFiat.title" to "mobile.openTradeNotifications.peerReceivedFiat.message"
                } else {
                    // User is seller -> they confirmed receiving payment
                    "mobile.openTradeNotifications.youReceivedFiat.title" to "mobile.openTradeNotifications.youReceivedFiat.message"
                }

                notify(
                    trade,
                    NotificationIds.getTradeStateUpdatedId(trade.shortTradeId),
                    titleKey.i18n(trade.shortTradeId),
                    messageKey.i18n(trade.peersUserName),
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

                notify(
                    trade,
                    NotificationIds.getTradeStateUpdatedId(trade.shortTradeId),
                    titleKey.i18n(trade.shortTradeId),
                    messageKey.i18n(trade.peersUserName),
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

                notify(
                    trade,
                    NotificationIds.getTradeStateUpdatedId(trade.shortTradeId),
                    titleKey.i18n(trade.shortTradeId),
                    messageKey.i18n(trade.peersUserName),
                )
            }

            // Early trade states that might be missed - offer taking notifications
            BisqEasyTradeStateEnum.TAKER_SENT_TAKE_OFFER_REQUEST -> {
                notify(
                    trade,
                    NotificationIds.getTradeStateUpdatedId(trade.shortTradeId),
                    "mobile.openTradeNotifications.offerTaken.title".i18n(trade.shortTradeId),
                    "mobile.openTradeNotifications.offerTaken.message".i18n(trade.peersUserName),
                )
            }

            // Maker states - when someone takes the user's offer (user is maker)
            BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
            BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA -> {
                notify(
                    trade,
                    NotificationIds.getTradeStateUpdatedId(trade.shortTradeId),
                    "mobile.openTradeNotifications.offerTaken.title".i18n(trade.shortTradeId),
                    "mobile.openTradeNotifications.offerTaken.message".i18n(trade.peersUserName),
                )
            }

            // States where payment account info is exchanged
            BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
            BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS_,
            BisqEasyTradeStateEnum.MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA -> {
                if (notifiedPaymentInfo.add(trade.shortTradeId)) {
                    // Determine if user sent or received payment info based on trade role
                    val (titleKey, messageKey) = if (trade.bisqEasyTradeModel.isSeller) {
                        // User is seller -> they sent payment info
                        "mobile.openTradeNotifications.paymentInfoSent.title" to "mobile.openTradeNotifications.paymentInfoSent.message"
                    } else {
                        // User is buyer -> they received payment info
                        "mobile.openTradeNotifications.paymentInfoReceived.title" to "mobile.openTradeNotifications.paymentInfoReceived.message"
                    }

                    notify(
                        trade,
                        NotificationIds.getTradeStateUpdatedId(trade.shortTradeId),
                        titleKey.i18n(trade.shortTradeId),
                        messageKey.i18n(trade.peersUserName),
                    )
                }
            }

            else -> {
                if (OffersServiceFacade.Companion.isTerminalState(state)) {
                    val translatedState = translatedI18N(state)
                    notify(
                        trade,
                        NotificationIds.getTradeStateUpdatedId(trade.shortTradeId),
                        "mobile.openTradeNotifications.tradeCompleted.title".i18n(trade.shortTradeId),
                        "mobile.openTradeNotifications.tradeCompleted.message".i18n(
                            trade.peersUserName,
                            translatedState
                        ),
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

    private fun notify(
        trade: TradeItemPresentationModel,
        id: String,
        title: String,
        body: String,
        isChatNotif: Boolean = false
    ) {
        notificationController.notify {
            this.id = id
            this.title = title
            this.body = body
            android {
                channelId = if (isChatNotif) {
                    NotificationChannels.USER_MESSAGES
                } else {
                    NotificationChannels.TRADE_UPDATES
                }
                pressAction = if (isChatNotif) {
                    category = AndroidNotificationCategory.CATEGORY_MESSAGE
                    NotificationPressAction.Route(NavRoute.TradeChat(trade.tradeId))
                } else {
                    category = AndroidNotificationCategory.CATEGORY_PROGRESS
                    NotificationPressAction.Route(NavRoute.OpenTrade(trade.tradeId))
                }
                group = trade.shortTradeId
            }
            ios {
                pressAction = if (isChatNotif) {
                    NotificationPressAction.Route(NavRoute.TradeChat(trade.tradeId))
                } else {
                    NotificationPressAction.Route(NavRoute.OpenTrade(trade.tradeId))
                }
            }
        }
    }
}