package network.bisq.mobile.client.websocket

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.url
import io.ktor.util.collections.ConcurrentMap
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.websocket.messages.SubscriptionRequest
import network.bisq.mobile.client.websocket.messages.SubscriptionResponse
import network.bisq.mobile.client.websocket.messages.WebSocketEvent
import network.bisq.mobile.client.websocket.messages.WebSocketMessage
import network.bisq.mobile.client.websocket.messages.WebSocketRequest
import network.bisq.mobile.client.websocket.messages.WebSocketResponse
import network.bisq.mobile.client.websocket.messages.WebSocketRestApiRequest
import network.bisq.mobile.client.websocket.messages.WebSocketRestApiResponse
import network.bisq.mobile.client.websocket.subscription.ModificationType
import network.bisq.mobile.client.websocket.subscription.Topic
import network.bisq.mobile.client.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.common.currency.marketListDemoObj
import network.bisq.mobile.domain.data.replicated.common.monetary.CoinVO
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVO
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.domain.data.replicated.common.network.AddressByTransportTypeMapVO
import network.bisq.mobile.domain.data.replicated.identity.identitiesDemoObj
import network.bisq.mobile.domain.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.QuoteSideFixedAmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.domain.data.replicated.offer.payment_method.BitcoinPaymentMethodSpecVO
import network.bisq.mobile.domain.data.replicated.offer.payment_method.FiatPaymentMethodSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.domain.data.replicated.presentation.offerbook.OfferItemPresentationDto
import network.bisq.mobile.domain.data.replicated.security.keys.PubKeyVO
import network.bisq.mobile.domain.data.replicated.security.keys.PublicKeyVO
import network.bisq.mobile.domain.data.replicated.security.pow.ProofOfWorkVO
import network.bisq.mobile.domain.data.replicated.settings.apiVersionSettingsVO
import network.bisq.mobile.domain.data.replicated.settings.settingsVODemoObj
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.replicated.user.profile.userProfileDemoObj
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.domain.utils.createUuid

class WebSocketClient(
    private val httpClient: HttpClient,
    val json: Json,
    val host: String,
    val port: Int
) : Logging {

    companion object {
        // TODO we might want to make this configurable
        const val CONNECT_TIMEOUT = 10000L
        const val DEMO_URL = "ws://demo.bisq:21"
        const val DELAY_TO_RECONNECT = 3000L
        const val MAX_RECONNECT_ATTEMPTS = 5
        const val MAX_RECONNECT_DELAY = 30000L // 30 seconds max delay
    }

    // Add these properties to track reconnection state
    private var reconnectAttempts = 0
    private var isReconnecting = false
    private var reconnectJob: Job? = null

    private val webSocketUrl: String = "ws://$host:$port/websocket"
    private var session: DefaultClientWebSocketSession? = null
    private val webSocketEventObservers = ConcurrentMap<String, WebSocketEventObserver>()
    private val requestResponseHandlers = mutableMapOf<String, RequestResponseHandler>()
    private var connectionReady = CompletableDeferred<Boolean>()
    private val connectionMutex = Mutex()
    private val requestResponseHandlersMutex = Mutex()

    private val ioScope = CoroutineScope(IODispatcher)

    enum class WebSocketClientStatus {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    val _webSocketClientStatus = MutableStateFlow(WebSocketClientStatus.DISCONNECTED)
    val webSocketClientStatus: StateFlow<WebSocketClientStatus> = _webSocketClientStatus

    private var listenerJob: Job? = null

    fun isConnected(): Boolean = webSocketClientStatus.value == WebSocketClientStatus.CONNECTED || isDemo()

    fun isDemo(): Boolean = webSocketUrl.startsWith(DEMO_URL)

    suspend fun connect(isTest: Boolean = false) {
        var needReconnect = false
        connectionMutex.withLock {
            try {
                if (webSocketClientStatus.value == WebSocketClientStatus.CONNECTED) {
                    throw IllegalStateException("Cannot connect an already connected client, please call disconnect first")
                }
                connectionReady = CompletableDeferred()
                log.d { "WS connecting.." }
                _webSocketClientStatus.value = WebSocketClientStatus.CONNECTING
                val newSession = withTimeout(CONNECT_TIMEOUT) {
                    httpClient.webSocketSession { url(webSocketUrl) }
                }
                session = newSession
                if (session?.isActive == true) {
                    log.d { "WS connected successfully" }
                    _webSocketClientStatus.value = WebSocketClientStatus.CONNECTED
                    if (!isTest) {
                        listenerJob = ioScope.launch { startListening() }
                    }
                    if (!connectionReady.isCompleted) {
                        connectionReady.complete(true)
                    }

                    // Reset reconnect attempts on successful connection
                    reconnectAttempts = 0
                }
            } catch (e: IllegalStateException) {
                log.w { "Connection attempt ignored: ${e.message}" }
                if (isTest) {
                    throw e
                }
            } catch (e: Exception) {
                log.e("Connecting websocket failed $webSocketUrl: ${e.message}", e)
                _webSocketClientStatus.value = WebSocketClientStatus.DISCONNECTED
                needReconnect = !isTest
                if (isTest) {
                    throw e
                }
            }
            if (needReconnect) reconnect()
        }
    }

    /**
     * @param isTest true if the connection of this client was a test connection
     * @param isReconnect true if this was called from a reconnect method
     */
    suspend fun disconnect(isTest: Boolean = false, isReconnect: Boolean = false) {
        connectionMutex.withLock {
            log.d { "disconnecting socket isTest $isTest isReconnected $isReconnect" }
            if (!isReconnect) {
                reconnectJob?.cancel()
                reconnectJob = null
            }
            listenerJob?.cancel()
            listenerJob = null
            requestResponseHandlersMutex.withLock {
                requestResponseHandlers.values.forEach { it.dispose() }
                requestResponseHandlers.clear()
            }

            session?.close()
            session = null
            _webSocketClientStatus.value = WebSocketClientStatus.DISCONNECTED
            if (!isTest) {
                log.d { "WS disconnected" }
            }
        }
    }

    private fun reconnect() {
        if (isReconnecting) {
            log.d { "Reconnect already in progress, skipping" }
            return
        }
        reconnectJob?.cancel()
        
        reconnectJob = ioScope.launch {
            isReconnecting = true
            log.d { "Launching reconnect attempt #${reconnectAttempts + 1}" }
            
            try {
                // Implement exponential backoff
                val delayMillis = minOf(
                    DELAY_TO_RECONNECT * (1 shl minOf(reconnectAttempts, 4)), // Exponential backoff
                    MAX_RECONNECT_DELAY
                )
                
                log.d { "Waiting ${delayMillis}ms before reconnect attempt" }
                disconnect(false, true)
                delay(delayMillis)
                
                // Check if we've exceeded max attempts
                if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
                    log.w { "Maximum reconnect attempts ($MAX_RECONNECT_ATTEMPTS) reached" }
                    _webSocketClientStatus.value = WebSocketClientStatus.DISCONNECTED
                    // Reset counter for future reconnects
                    reconnectAttempts = 0
                    return@launch
                }
                
                reconnectAttempts++
                connect()
            } finally {
                isReconnecting = false
            }
        }
    }

    // Blocking request until we get the associated response
    suspend fun sendRequestAndAwaitResponse(webSocketRequest: WebSocketRequest): WebSocketResponse? {
        if (isDemo()) {
            return fakeResponse(webSocketRequest)
        }
        connectionReady.await()

        val requestId = webSocketRequest.requestId
        val requestResponseHandler = RequestResponseHandler(this::send)
        requestResponseHandlersMutex.withLock {
            requestResponseHandlers[requestId] = requestResponseHandler
        }

        try {
            return requestResponseHandler.request(webSocketRequest)
        } finally {
            requestResponseHandlersMutex.withLock {
                requestResponseHandlers.remove(requestId)
            }
        }
    }

    suspend fun await() = connectionReady.await()

    private fun fakeResponse(webSocketRequest: WebSocketRequest): WebSocketResponse {
        webSocketRequest as WebSocketRestApiRequest
        log.d { "responding fake response to path ${webSocketRequest.path}" }
        return WebSocketRestApiResponse(
            webSocketRequest.requestId, 200,
            body = when {
                webSocketRequest.path.endsWith("settings") -> json.encodeToString(settingsVODemoObj)
                webSocketRequest.path.endsWith("settings/version") -> json.encodeToString(apiVersionSettingsVO)
                webSocketRequest.path.endsWith("user-identities/ids") -> json.encodeToString(identitiesDemoObj)
                webSocketRequest.path.endsWith("offerbook/markets") -> json.encodeToString(marketListDemoObj)
                webSocketRequest.path.endsWith("selected/user-profile") -> json.encodeToString(userProfileDemoObj)
                else -> "{}"
            }
        )
    }

    suspend fun subscribe(topic: Topic, parameter: String? = null): WebSocketEventObserver {
        val subscriberId = createUuid()
        log.i { "Subscribe for topic $topic and subscriberId $subscriberId" }

        if (isDemo()) {
            log.i { "Demo mode active. Returning fake data for topic $topic." }
            return getFakeSubscription(topic, subscriberId)
        }

        val subscriptionRequest = SubscriptionRequest(
            subscriberId,
            topic,
            parameter
        )
        val response: WebSocketResponse? = sendRequestAndAwaitResponse(subscriptionRequest)
        require(response is SubscriptionResponse)
        log.i {
            "Received SubscriptionResponse for topic $topic and subscriberId $subscriberId."
        }
        val webSocketEventObserver = WebSocketEventObserver()
        webSocketEventObservers[subscriberId] = webSocketEventObserver
        val webSocketEvent = WebSocketEvent(
            topic,
            subscriberId,
            response.payload,
            ModificationType.REPLACE,
            0
        )
        webSocketEventObserver.setEvent(webSocketEvent)
        log.i { "Subscription for $topic and subscriberId $subscriberId completed." }
        return webSocketEventObserver
    }

    suspend fun unSubscribe(topic: Topic, requestId: String) {
        //todo
    }

    private suspend fun send(message: WebSocketMessage) {
        connectionReady.await()
        log.i { "Send message $message" }
        val jsonString: String = json.encodeToString(message)
        log.i { "Send raw text $jsonString" }
        session?.send(Frame.Text(jsonString))
    }

    suspend fun publicSend(subscriberId: String, topic: Topic, parameter: String? = null) {
        val subscriptionRequest = SubscriptionRequest(
            subscriberId,
            topic,
            parameter
        );
        send(subscriptionRequest)
    }

    private suspend fun startListening() {
        session?.let { session ->
            try {
                for (frame in session.incoming) {
                    if (frame is Frame.Text) {
                        val message = frame.readText()
                        //todo add input validation
                        log.d { "Received raw text $message" }
                        val webSocketMessage: WebSocketMessage =
                            json.decodeFromString(WebSocketMessage.serializer(), message)
                        log.i { "Received webSocketMessage $webSocketMessage" }
                        if (webSocketMessage is WebSocketResponse) {
                            onWebSocketResponse(webSocketMessage)
                        } else if (webSocketMessage is WebSocketEvent) {
                            onWebSocketEvent(webSocketMessage)
                        }
                    }
                }
                
                // If we get here, the loop exited normally (session closed gracefully)
                log.d { "WebSocket session closed normally" }
                _webSocketClientStatus.value = WebSocketClientStatus.DISCONNECTED
                
            } catch (e: Exception) {
                log.e(e) { "Exception occurred whilst listening for WS messages - triggering reconnect" }
                // Only reconnect on exception
//                TODO this needs more work
//                reconnect()
            }
        }
    }

    private suspend fun onWebSocketResponse(response: WebSocketResponse) {
        requestResponseHandlers[response.requestId]?.onWebSocketResponse(response)
    }

    private fun onWebSocketEvent(event: WebSocketEvent) {
        // We have the payload not serialized yet as we would not know the expected type.
        // We delegate that at the caller who is aware of the expected type
        val webSocketEventObserver = webSocketEventObservers[event.subscriberId]
        if (webSocketEventObserver != null) {
            webSocketEventObserver.setEvent(event)
        } else {
            log.w { "We received a WebSocketEvent but no webSocketEventObserver was found for subscriberId ${event.subscriberId}" }
        }
    }

    // Function to return fake data when in demo mode
    private fun getFakeSubscription(topic: Topic, subscriberId: String): WebSocketEventObserver {
        val fakePayload = getFakePayloadForTopic(topic) // Function that returns fake data
        val webSocketEventObserver = WebSocketEventObserver()

        val webSocketEvent = WebSocketEvent(topic, subscriberId, fakePayload, ModificationType.REPLACE, 0)
        webSocketEventObserver.setEvent(webSocketEvent)

        return webSocketEventObserver
    }

    // Define fake data for each topic
    private fun getFakePayloadForTopic(topic: Topic): String? {
        return when (topic) {
            Topic.MARKET_PRICE -> Json.encodeToString(FakeSubscriptionData.marketPrice)
            Topic.NUM_OFFERS -> Json.encodeToString(FakeSubscriptionData.numOffers)
            Topic.OFFERS -> Json.encodeToString(FakeSubscriptionData.offers)
//            Topic.TRADES -> Json.encodeToString(FakeData.trades)
//            Topic.TRADE_PROPERTIES -> Json.encodeToString(FakeData.tradeProps)
            else -> null // Default empty response
        }
    }

    // TODO refactor our of websocket client
    // Example fake data
    object FakeSubscriptionData {
        val marketPrice = mapOf(
            "USD" to PriceQuoteVO(
                80000,
                4,
                2,
                MarketVO("Bitcoin", "USD"),
                CoinVO("BTC", 1, "BTC", 8, 4),
                FiatVO("USD", 80000, "USD", 4, 2)
            ),
            "EUR" to PriceQuoteVO(
                75000,
                4,
                2,
                MarketVO("Bitcoin", "EUR"),
                CoinVO("BTC", 1, "BTC", 8, 4),
                FiatVO("EUR", 75000, "EUR", 4, 2)
            ),
        )
        val trades = mapOf("BTC" to "0.5", "USD" to "10000")
        val offers = listOf(
            OfferItemPresentationDto(
                bisqEasyOffer = BisqEasyOfferVO(
                    id = "1",
                    date = 1741912747L,
                    makerNetworkId = NetworkIdVO(
                        addressByTransportTypeMap = AddressByTransportTypeMapVO(
                            map = mapOf()
                        ),
                        pubKey = PubKeyVO(
                            publicKey = PublicKeyVO(
                                encoded = "makerpub"
                            ),
                            keyId = "makerkey",
                            hash = "makerhash",
                            id = "maker"
                        )
                    ),
                    direction = DirectionEnum.SELL,
                    market = MarketVO("Bitcoin", "USD"),
                    amountSpec = QuoteSideFixedAmountSpecVO(
                        amount = 100
                    ),
                    priceSpec = FixPriceSpecVO(
                        priceQuote = PriceQuoteVO(
                            value = 100L,
                            4,
                            2,
                            market = MarketVO(
                                baseCurrencyCode = "Bitcoin",
                                quoteCurrencyCode = "USD",
                            ),
                            CoinVO("BTC", 1, "BTC", 8, 4),
                            FiatVO("USD", 100L, "USD", 4, 2),
                        )
                    ),
                    protocolTypes = listOf(),
                    baseSidePaymentMethodSpecs = listOf(
                        BitcoinPaymentMethodSpecVO(
                            paymentMethod = "onchain",
                            saltedMakerAccountId = "onchain"
                        )
                    ),
                    quoteSidePaymentMethodSpecs = listOf(
                        FiatPaymentMethodSpecVO(
                            paymentMethod = "payid",
                            saltedMakerAccountId = "payid"
                        )
                    ),
                    offerOptions = listOf(),
                    supportedLanguageCodes = listOf("EN")
                ),
                isMyOffer = false,
                userProfile = UserProfileVO(
                    1, "pepe",
                    ProofOfWorkVO(
                        payloadEncoded = "payme",
                        counter = 1L,
                        challengeEncoded = "challenge",
                        difficulty = 2.0,
                        solutionEncoded = "solution",
                        duration = 2000L
                    ),
                    avatarVersion = 1,
                    networkId = NetworkIdVO(
                        addressByTransportTypeMap = AddressByTransportTypeMapVO(map = mapOf()),
                        pubKey = PubKeyVO(
                            publicKey = PublicKeyVO("encoded"),
                            keyId = "keyid",
                            hash = "hash",
                            id = "id"
                        )
                    ),
                    terms = "",
                    statement = "",
                    applicationVersion = "",
                    nym = "mynym",
                    userName = "pepito",
                    publishDate = 1741212747L,
                ),
                formattedDate = "",
                formattedQuoteAmount = "",
                formattedBaseAmount = "",
                formattedPrice = "",
                formattedPriceSpec = "",
                quoteSidePaymentMethods = listOf("onchain"),
                baseSidePaymentMethods = listOf("payid"),
                reputationScore = ReputationScoreVO(
                    totalScore = 12,
                    fiveSystemScore = 22.0,
                    ranking = 4
                ),
            ),
            OfferItemPresentationDto(
                bisqEasyOffer = BisqEasyOfferVO(
                    id = "2",
                    date = 1741922747L,
                    makerNetworkId = NetworkIdVO(
                        addressByTransportTypeMap = AddressByTransportTypeMapVO(
                            map = mapOf()
                        ),
                        pubKey = PubKeyVO(
                            publicKey = PublicKeyVO(
                                encoded = "makerpub"
                            ),
                            keyId = "makerkey",
                            hash = "makerhash",
                            id = "maker"
                        )
                    ),
                    direction = DirectionEnum.BUY,
                    market = MarketVO("Bitcoin", "USD"),
                    amountSpec = QuoteSideFixedAmountSpecVO(
                        amount = 102
                    ),
                    priceSpec = FixPriceSpecVO(
                        priceQuote = PriceQuoteVO(
                            value = 102L,
                            4,
                            2,
                            market = MarketVO(
                                baseCurrencyCode = "Bitcoin",
                                quoteCurrencyCode = "USD",
                            ),
                            CoinVO("BTC", 1, "BTC", 8, 4),
                            FiatVO("USD", 102L, "USD", 4, 2),
                        )
                    ),
                    protocolTypes = listOf(),
                    baseSidePaymentMethodSpecs = listOf(
                        BitcoinPaymentMethodSpecVO(
                            paymentMethod = "onchain",
                            saltedMakerAccountId = "onchain"
                        )
                    ),
                    quoteSidePaymentMethodSpecs = listOf(
                        FiatPaymentMethodSpecVO(
                            paymentMethod = "payid",
                            saltedMakerAccountId = "payid"
                        )
                    ),
                    offerOptions = listOf(),
                    supportedLanguageCodes = listOf("EN")
                ),
                isMyOffer = true,
                userProfile = UserProfileVO(
                    1, "pepe",
                    ProofOfWorkVO(
                        payloadEncoded = "payme",
                        counter = 1L,
                        challengeEncoded = "challenge",
                        difficulty = 2.0,
                        solutionEncoded = "solution",
                        duration = 2000L
                    ),
                    avatarVersion = 1,
                    networkId = NetworkIdVO(
                        addressByTransportTypeMap = AddressByTransportTypeMapVO(map = mapOf()),
                        pubKey = PubKeyVO(
                            publicKey = PublicKeyVO("encoded"),
                            keyId = "keyid",
                            hash = "hash",
                            id = "id"
                        )
                    ),
                    terms = "",
                    statement = "",
                    applicationVersion = "",
                    nym = "mynym",
                    userName = "myoffer",
                    publishDate = 1741212747L,
                ),
                formattedDate = "",
                formattedQuoteAmount = "",
                formattedBaseAmount = "",
                formattedPrice = "",
                formattedPriceSpec = "",
                quoteSidePaymentMethods = listOf("onchain"),
                baseSidePaymentMethods = listOf("payid"),
                reputationScore = ReputationScoreVO(
                    totalScore = 12,
                    fiveSystemScore = 22.0,
                    ranking = 4
                ),
            )
        )
        val numOffers = mapOf("USD" to offers.size)
    }
}