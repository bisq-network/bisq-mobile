package network.bisq.mobile.client.websocket

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.url
import io.ktor.util.collections.ConcurrentMap
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.common.currency.marketListDemoObj
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.domain.data.replicated.common.network.AddressByTransportTypeMapVO
import network.bisq.mobile.domain.data.replicated.identity.identitiesDemoObj
import network.bisq.mobile.domain.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.AmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.BaseSideFixedAmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.QuoteSideFixedAmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.domain.data.replicated.offer.payment_method.BitcoinPaymentMethodSpecVO
import network.bisq.mobile.domain.data.replicated.offer.payment_method.FiatPaymentMethodSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.domain.data.replicated.presentation.offerbook.OfferItemPresentationDto
import network.bisq.mobile.domain.data.replicated.security.keys.PubKeyVO
import network.bisq.mobile.domain.data.replicated.security.keys.PublicKeyVO
import network.bisq.mobile.domain.data.replicated.security.pow.ProofOfWorkVO
import network.bisq.mobile.domain.data.replicated.settings.settingsVODemoObj
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
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
        const val DEMO_URL = "ws://demo.bisq:21"
        const val DELAY_TO_RECONNECT = 3000L
    }

    private val webSocketUrl: String = "ws://$host:$port/websocket"
    private var session: DefaultClientWebSocketSession? = null
    private val webSocketEventObservers = ConcurrentMap<String, WebSocketEventObserver>()
    private val requestResponseHandlers = mutableMapOf<String, RequestResponseHandler>()
    private var connectionReady = CompletableDeferred<Boolean>()
    private val requestResponseHandlersMutex = Mutex()

    private val backgroundScope = CoroutineScope(BackgroundDispatcher)

    enum class WebSockectClientStatus {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    val _connected = MutableStateFlow(WebSockectClientStatus.DISCONNECTED)
    val connected: StateFlow<WebSockectClientStatus> = _connected

    fun isConnected(): Boolean = connected.value == WebSockectClientStatus.CONNECTED || isDemo()

    fun isDemo(): Boolean = webSocketUrl.startsWith(DEMO_URL)

    suspend fun connect(isTest: Boolean = false) {
        log.i("Connecting to websocket at: $webSocketUrl")
        if (connected.value != WebSockectClientStatus.CONNECTED) {
            try {
                _connected.value = WebSockectClientStatus.CONNECTING
                session = httpClient.webSocketSession { url(webSocketUrl) }
                if (session?.isActive == true) {
                    _connected.value = WebSockectClientStatus.CONNECTED
                    CoroutineScope(BackgroundDispatcher).launch { startListening() }
                    connectionReady.complete(true)
                    if (!isTest) {
                        log.d { "Websocket connected" }
                    }
                }
            } catch (e: Exception) {
                log.e("Connecting websocket failed", e)
                _connected.value = WebSockectClientStatus.DISCONNECTED
                if (isTest) {
                    throw e
                } else {
                    reconnect()
                }
            }
        }
    }

    suspend fun disconnect(isTest: Boolean = false) {
        requestResponseHandlersMutex.withLock {
            requestResponseHandlers.values.forEach { it.dispose() }
            requestResponseHandlers.clear()
        }

        session?.close()
        session = null
        _connected.value = WebSockectClientStatus.DISCONNECTED
        if (!isTest) {
            log.d { "WS disconnected" }
        }
    }

    private fun reconnect() {
        backgroundScope.launch {
            log.d { "Launching reconnect" }
            disconnect()
            delay(DELAY_TO_RECONNECT) // Delay before reconnecting
            connect()  // Try reconnecting recursively
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

    private fun fakeResponse(webSocketRequest: WebSocketRequest): WebSocketResponse {
        webSocketRequest as WebSocketRestApiRequest
        log.d { "responding fake response to path ${webSocketRequest.path}" }
        return WebSocketRestApiResponse(webSocketRequest.requestId, 200,
            body = when {
                webSocketRequest.path.endsWith("settings") -> json.encodeToString(settingsVODemoObj)
                webSocketRequest.path.endsWith("user-identities/ids") -> json.encodeToString(identitiesDemoObj)
                webSocketRequest.path.endsWith("offerbook/markets") -> json.encodeToString(marketListDemoObj)
                else -> "{}"
            })
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
            } catch (e: Exception) {
                log.e(e) { "Exception ocurred whilst listening for WS messages - triggering reconnect" }
            } finally {
                log.d { "Not listening for WS messages anymore - launching reconnect" }
                reconnect()
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
            "USD" to PriceQuoteVO(80000, MarketVO("Bitcoin", "USD")),
            "EUR" to PriceQuoteVO(75000, MarketVO("Bitcoin", "EUR")),
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
                            market = MarketVO(
                                baseCurrencyCode = "Bitcoin",
                                quoteCurrencyCode = "USD",
                            )
                        )
                    ),
                    protocolTypes = listOf(),
                    baseSidePaymentMethodSpecs = listOf(BitcoinPaymentMethodSpecVO(
                        paymentMethod = "onchain",
                        saltedMakerAccountId = "onchain"
                    )),
                    quoteSidePaymentMethodSpecs = listOf(FiatPaymentMethodSpecVO(
                        paymentMethod = "payid",
                        saltedMakerAccountId = "payid"
                    )),
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
                            market = MarketVO(
                                baseCurrencyCode = "Bitcoin",
                                quoteCurrencyCode = "USD",
                            )
                        )
                    ),
                    protocolTypes = listOf(),
                    baseSidePaymentMethodSpecs = listOf(BitcoinPaymentMethodSpecVO(
                        paymentMethod = "onchain",
                        saltedMakerAccountId = "onchain"
                    )),
                    quoteSidePaymentMethodSpecs = listOf(FiatPaymentMethodSpecVO(
                        paymentMethod = "payid",
                        saltedMakerAccountId = "payid"
                    )),
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