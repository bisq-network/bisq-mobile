package network.bisq.mobile.client.websocket

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.websocket.messages.WebSocketEvent
import network.bisq.mobile.client.websocket.messages.WebSocketRequest
import network.bisq.mobile.client.websocket.messages.WebSocketResponse
import network.bisq.mobile.client.websocket.messages.WebSocketRestApiRequest
import network.bisq.mobile.client.websocket.messages.WebSocketRestApiResponse
import network.bisq.mobile.client.websocket.subscription.ModificationType
import network.bisq.mobile.client.websocket.subscription.Topic
import network.bisq.mobile.client.websocket.subscription.WebSocketEventObserver
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

class WebSocketClientDemo(
    private val json: Json,
) : WebSocketClient, Logging {

    override val host = "demo.bisq"
    override val port = 21

    private val _webSocketClientStatus =
        MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())
    override val webSocketClientStatus: StateFlow<ConnectionState> get() = _webSocketClientStatus.asStateFlow()

    override fun isDemo(): Boolean = true

    override suspend fun connect(isTest: Boolean) {
        log.d { "Demo mode detected - skipping actual WebSocket connection" }
        _webSocketClientStatus.value = ConnectionState.Connected
    }

    override suspend fun disconnect(isTest: Boolean, isReconnect: Boolean) {
        log.d { "Demo mode - simulating disconnect" }
        _webSocketClientStatus.value = ConnectionState.Disconnected()
    }

    override fun reconnect() {
        log.d { "Demo mode - skipping reconnect" }
    }

    override suspend fun sendRequestAndAwaitResponse(webSocketRequest: WebSocketRequest): WebSocketResponse? {
        return fakeResponse(webSocketRequest)
    }

    override suspend fun awaitConnection() {
        webSocketClientStatus.first { it is ConnectionState.Connected }
    }

    override suspend fun subscribe(
        topic: Topic,
        parameter: String?
    ): WebSocketEventObserver {
        val subscriberId = createUuid()
        log.i { "Subscribe for topic $topic and subscriberId $subscriberId" }
        log.i { "Demo mode active. Returning fake data for topic $topic." }
        return getFakeSubscription(topic, subscriberId)
    }

    override suspend fun unSubscribe(
        topic: Topic,
        requestId: String
    ) {
        log.d { "Demo mode - unsubscribe ignored for topic=$topic, requestId=$requestId" }
        // no-op, TODO
    }

    private fun fakeResponse(webSocketRequest: WebSocketRequest): WebSocketResponse {
        webSocketRequest as WebSocketRestApiRequest
        log.d { "responding fake response to path ${webSocketRequest.path}" }
        return WebSocketRestApiResponse(
            webSocketRequest.requestId, 200,
            body = when {
                webSocketRequest.path.endsWith("settings") -> json.encodeToString(settingsVODemoObj)
                webSocketRequest.path.endsWith("settings/version") -> json.encodeToString(
                    apiVersionSettingsVO
                )

                webSocketRequest.path.endsWith("user-identities/ids") -> json.encodeToString(
                    identitiesDemoObj
                )

                webSocketRequest.path.endsWith("offerbook/markets") -> json.encodeToString(
                    marketListDemoObj
                )

                webSocketRequest.path.endsWith("selected/user-profile") -> json.encodeToString(
                    userProfileDemoObj
                )

                else -> "{}"
            }
        )
    }

    // Function to return fake data when in demo mode
    private fun getFakeSubscription(topic: Topic, subscriberId: String): WebSocketEventObserver {
        val fakePayload = getFakePayloadForTopic(topic) // Function that returns fake data
        val webSocketEventObserver = WebSocketEventObserver()

        val webSocketEvent =
            WebSocketEvent(topic, subscriberId, fakePayload, ModificationType.REPLACE, 0)
        webSocketEventObserver.setEvent(webSocketEvent)

        return webSocketEventObserver
    }

    // Define fake data for each topic
    private fun getFakePayloadForTopic(topic: Topic): String? {
        return when (topic) {
            Topic.MARKET_PRICE -> json.encodeToString(FakeSubscriptionData.marketPrice)
            Topic.NUM_OFFERS -> json.encodeToString(FakeSubscriptionData.numOffers)
            Topic.OFFERS -> json.encodeToString(FakeSubscriptionData.offers)
//            Topic.TRADES -> json.encodeToString(FakeData.trades)
//            Topic.TRADE_PROPERTIES -> json.encodeToString(FakeData.tradeProps)
            else -> null // Default empty response
        }
    }
}

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