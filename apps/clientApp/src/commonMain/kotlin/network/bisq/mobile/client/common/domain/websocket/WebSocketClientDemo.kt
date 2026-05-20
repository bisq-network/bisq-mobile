package network.bisq.mobile.client.common.domain.websocket

import io.ktor.http.parseUrl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketEvent
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketRequest
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketResponse
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketRestApiRequest
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketRestApiResponse
import network.bisq.mobile.client.common.domain.websocket.subscription.ModificationType
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.replicated.common.currency.marketListDemoObj
import network.bisq.mobile.data.replicated.common.monetary.CoinVO
import network.bisq.mobile.data.replicated.common.monetary.FiatVO
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.data.replicated.common.network.AddressByTransportTypeMapVO
import network.bisq.mobile.data.replicated.identity.identitiesDemoObj
import network.bisq.mobile.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.data.replicated.offer.amount.spec.QuoteSideFixedAmountSpecVO
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.data.replicated.offer.payment_method.BitcoinPaymentMethodSpecVO
import network.bisq.mobile.data.replicated.offer.payment_method.FiatPaymentMethodSpecVO
import network.bisq.mobile.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationDto
import network.bisq.mobile.data.replicated.security.keys.PubKeyVO
import network.bisq.mobile.data.replicated.security.keys.PublicKeyVO
import network.bisq.mobile.data.replicated.security.pow.ProofOfWorkVO
import network.bisq.mobile.data.replicated.settings.apiVersionSettingsVO
import network.bisq.mobile.data.replicated.settings.settingsVODemoObj
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.userProfileDemoObj
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.domain.utils.createUuid

class WebSocketClientDemo(
    private val json: Json,
) : WebSocketClient,
    Logging {
    override val apiUrl = parseUrl("http://demo.bisq:21")!!

    private val _webSocketClientStatus =
        MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())
    override val webSocketClientStatus: StateFlow<ConnectionState> = _webSocketClientStatus.asStateFlow()

    override fun isDemo(): Boolean = true

    override suspend fun connect(timeout: Long): Throwable? {
        log.d { "Demo mode detected - skipping actual WebSocket connection" }
        _webSocketClientStatus.value = ConnectionState.Connected
        return null
    }

    override suspend fun disconnect() {
        log.d { "Demo mode - simulating disconnect" }
        _webSocketClientStatus.value = ConnectionState.Disconnected()
    }

    override fun reconnect() {
        log.d { "Demo mode - skipping reconnect" }
    }

    override suspend fun sendRequestAndAwaitResponse(
        webSocketRequest: WebSocketRequest,
        awaitConnection: Boolean,
    ): WebSocketResponse? = fakeResponse(webSocketRequest)

    override suspend fun subscribe(
        topic: Topic,
        parameter: String?,
        webSocketEventObserver: WebSocketEventObserver,
    ): WebSocketEventObserver {
        val subscriberId = createUuid()
        log.i { "Subscribe for topic $topic and subscriberId $subscriberId" }
        log.i { "Demo mode active. Returning fake data for topic $topic." }
        return getFakeSubscription(topic, subscriberId, webSocketEventObserver)
    }

    override suspend fun unSubscribe(
        topic: Topic,
        requestId: String,
    ) {
        log.d { "Demo mode - unsubscribe ignored for topic=$topic, requestId=$requestId" }
        // no-op, TODO
    }

    override suspend fun dispose() {
        // no-op
    }

    private fun fakeResponse(webSocketRequest: WebSocketRequest): WebSocketResponse {
        webSocketRequest as WebSocketRestApiRequest
        log.d { "Demo: responding fake response to path ${webSocketRequest.path}" }
        val body =
            when {
                // Settings
                webSocketRequest.path.endsWith("settings") -> json.encodeToString(settingsVODemoObj)
                webSocketRequest.path.endsWith("settings/version") ->
                    json.encodeToString(apiVersionSettingsVO)

                // User identities
                webSocketRequest.path.endsWith("user-identities/ids") ->
                    json.encodeToString(identitiesDemoObj)
                webSocketRequest.path.endsWith("owned-profiles") ->
                    json.encodeToString(listOf(userProfileDemoObj))
                webSocketRequest.path.endsWith("selected/user-profile") ->
                    json.encodeToString(userProfileDemoObj)

                // User profiles - endpoints that return List<String> or List<UserProfileVO>
                webSocketRequest.path.endsWith("user-profiles/ignored") -> "[]"
                webSocketRequest.path.contains("user-profiles?ids=") ->
                    json.encodeToString(listOf(userProfileDemoObj))

                // Offerbook
                webSocketRequest.path.endsWith("offerbook/markets") ->
                    json.encodeToString(marketListDemoObj)

                // Payment accounts - returns List<FiatAccount>
                webSocketRequest.path.contains("payment-accounts/fiat") -> "[]"

                // Reputation - return null-safe defaults
                webSocketRequest.path.contains("reputation/profile-age/") -> "0"
                webSocketRequest.path.contains("reputation/score/") ->
                    json.encodeToString(ReputationScoreVO(totalScore = 0, fiveSystemScore = 0.0, ranking = 0))

                else -> {
                    log.w { "Demo: unhandled path ${webSocketRequest.path}, returning empty array" }
                    "[]" // Return empty array by default to avoid JSON parsing errors
                }
            }
        log.d { "Demo: response body length=${body.length} for path ${webSocketRequest.path}" }
        return WebSocketRestApiResponse(
            webSocketRequest.requestId,
            200,
            body = body,
        )
    }

    // Function to return fake data when in demo mode
    private suspend fun getFakeSubscription(
        topic: Topic,
        subscriberId: String,
        webSocketEventObserver: WebSocketEventObserver,
    ): WebSocketEventObserver {
        val fakePayload = getFakePayloadForTopic(topic) // Function that returns fake data
        log.d { "Demo: getFakeSubscription for topic=$topic, payload length=${fakePayload?.length ?: 0}" }
        if (fakePayload == null) {
            log.w { "Demo: No fake payload defined for topic=$topic" }
            return webSocketEventObserver
        }

        val webSocketEvent =
            WebSocketEvent(topic, subscriberId, fakePayload, ModificationType.REPLACE, 0)
        log.d { "Demo: Setting event for topic=$topic with sequenceNumber=0" }
        webSocketEventObserver.setEvent(webSocketEvent)
        log.d { "Demo: Event set for topic=$topic" }

        return webSocketEventObserver
    }

    // Define fake data for each topic
    private fun getFakePayloadForTopic(topic: Topic): String? =
        when (topic) {
            Topic.MARKET_PRICE -> json.encodeToString(FakeSubscriptionData.marketPrice)
            Topic.NUM_OFFERS -> json.encodeToString(FakeSubscriptionData.numOffers)
            Topic.OFFERS -> json.encodeToString(FakeSubscriptionData.offers)
            // initialSubscriptionsReceivedData in WebSocketClientService combines this with
            // MARKET_PRICE + NUM_OFFERS — without it the bootstrap progress UI hangs on
            // "Requesting initial network data" forever in demo mode.
            Topic.NUM_USER_PROFILES -> json.encodeToString(FakeSubscriptionData.NUM_USER_PROFILES)
//            Topic.TRADES -> json.encodeToString(FakeData.trades)
//            Topic.TRADE_PROPERTIES -> json.encodeToString(FakeData.tradeProps)
            else -> null // Default empty response
        }
}

// Example fake data
object FakeSubscriptionData {
    // Plausible BTC prices per quote currency. Values are minor-unit-scaled (×100 for
    // 2-decimal currencies). These don't have to be perfectly accurate — they're
    // displayed via the formatting code at runtime, but they should "look right" for
    // App Store screenshots.
    private val pricePerQuoteCurrency: Map<String, Long> =
        mapOf(
            "USD" to 9_500_000L, // ≈ 95,000 USD/BTC
            "EUR" to 8_800_000L, // ≈ 88,000 EUR/BTC
            "ARS" to 9_800_000_000_000L, // high-inflation currency
            "PYG" to 7_200_000_000_000L,
            "LBP" to 850_000_000_000_000L,
            "CZK" to 220_000_000L, // ≈ 2,200,000 CZK/BTC
            "AUD" to 14_500_000L, // ≈ 145,000 AUD/BTC
            "CAD" to 13_000_000L, // ≈ 130,000 CAD/BTC
            "IDR" to 150_000_000_000L,
        )

    val marketPrice: Map<String, PriceQuoteVO> =
        pricePerQuoteCurrency.mapValues { (quoteCode, value) ->
            PriceQuoteVO(
                value,
                4,
                2,
                MarketVO("BTC", quoteCode),
                CoinVO("BTC", 1, "BTC", 8, 4),
                FiatVO(quoteCode, value, quoteCode, 4, 2),
            )
        }

    val trades = mapOf("BTC" to "0.5", "USD" to "10000")

    private data class OfferSpec(
        val quote: String,
        val direction: DirectionEnum,
        val fiatAmount: Long,
        val makerKey: String,
        val makerNickName: String,
        val makerUserName: String,
        val isMyOffer: Boolean = false,
        val fiatPaymentMethod: String = "SEPA",
        val bitcoinPaymentMethod: String = "MAIN_CHAIN",
        val reputationTotalScore: Long = 25_000L,
        val ranking: Int = 100,
        val daysAgo: Int = 1,
    )

    // ~23 offers across 9 markets, mixed buy/sell, varied users + payment methods.
    // Each maker gets a unique pubKey hash so the cathash service produces a distinct
    // avatar per offer (otherwise the offerbook screenshot would show duplicates).
    private val offerSpecs: List<OfferSpec> =
        listOf(
            // USD — most active market, mix of payment apps
            OfferSpec("USD", DirectionEnum.SELL, 100, "alice", "Alice", "alice_btc", fiatPaymentMethod = "ZELLE", reputationTotalScore = 91_000, ranking = 8),
            OfferSpec("USD", DirectionEnum.BUY, 250, "bob", "Bob", "bobsly", fiatPaymentMethod = "REVOLUT", reputationTotalScore = 56_000, ranking = 24),
            OfferSpec("USD", DirectionEnum.SELL, 500, "carol", "Carol", "carol88", isMyOffer = true, fiatPaymentMethod = "ZELLE", reputationTotalScore = 84_000, ranking = 12, daysAgo = 0),
            OfferSpec("USD", DirectionEnum.BUY, 1_000, "dave", "Dave", "satoshislave", fiatPaymentMethod = "VENMO", reputationTotalScore = 42_000, ranking = 41, daysAgo = 2),
            // EUR — SEPA region
            OfferSpec("EUR", DirectionEnum.SELL, 200, "elena", "Elena", "elena_eu", fiatPaymentMethod = "SEPA", reputationTotalScore = 78_000, ranking = 14),
            OfferSpec("EUR", DirectionEnum.BUY, 350, "frank", "Frank", "frank_btc", fiatPaymentMethod = "REVOLUT", reputationTotalScore = 51_000, ranking = 28),
            OfferSpec("EUR", DirectionEnum.SELL, 800, "gabriela", "Gabriela", "gabby_p2p", fiatPaymentMethod = "WISE", reputationTotalScore = 33_000, ranking = 56, daysAgo = 3),
            OfferSpec("EUR", DirectionEnum.BUY, 150, "hiro", "Hiro", "hiro_node", fiatPaymentMethod = "SEPA", reputationTotalScore = 95_000, ranking = 3, daysAgo = 0),
            // AUD
            OfferSpec("AUD", DirectionEnum.SELL, 300, "ingrid", "Ingrid", "ingrid_au", fiatPaymentMethod = "OSKO", reputationTotalScore = 67_000, ranking = 19),
            OfferSpec("AUD", DirectionEnum.BUY, 600, "juan", "Juan", "juan_au", fiatPaymentMethod = "BPAY", reputationTotalScore = 18_000, ranking = 88, daysAgo = 4),
            OfferSpec("AUD", DirectionEnum.SELL, 1_500, "kira", "Kira", "kira_btc", fiatPaymentMethod = "OSKO", reputationTotalScore = 72_000, ranking = 17),
            // CAD
            OfferSpec("CAD", DirectionEnum.SELL, 400, "liam", "Liam", "liam_p2p", fiatPaymentMethod = "INTERAC_E_TRANSFER", reputationTotalScore = 60_000, ranking = 22),
            OfferSpec("CAD", DirectionEnum.BUY, 750, "maria", "Maria", "maria_north", fiatPaymentMethod = "INTERAC_E_TRANSFER", reputationTotalScore = 39_000, ranking = 47, daysAgo = 1),
            OfferSpec("CAD", DirectionEnum.SELL, 200, "noah", "Noah", "noah_btc", fiatPaymentMethod = "INTERAC_E_TRANSFER", bitcoinPaymentMethod = "LIGHTNING", reputationTotalScore = 88_000, ranking = 6, daysAgo = 0),
            // ARS
            OfferSpec("ARS", DirectionEnum.BUY, 50_000, "olivia", "Olivia", "oli_ar", fiatPaymentMethod = "MERCADO_PAGO", reputationTotalScore = 47_000, ranking = 33),
            OfferSpec("ARS", DirectionEnum.SELL, 100_000, "pedro", "Pedro", "pedro_ar", fiatPaymentMethod = "CASH_DEPOSIT", reputationTotalScore = 22_000, ranking = 71, daysAgo = 5),
            OfferSpec("ARS", DirectionEnum.BUY, 250_000, "quinn", "Quinn", "quinn_btc", fiatPaymentMethod = "MERCADO_PAGO", reputationTotalScore = 65_000, ranking = 21),
            // CZK
            OfferSpec("CZK", DirectionEnum.SELL, 2_500, "rosa", "Rosa", "rosa_cz", fiatPaymentMethod = "BANK_TRANSFER", reputationTotalScore = 54_000, ranking = 26),
            OfferSpec("CZK", DirectionEnum.BUY, 5_000, "sven", "Sven", "sven_p2p", fiatPaymentMethod = "REVOLUT", reputationTotalScore = 31_000, ranking = 58, daysAgo = 2),
            // IDR
            OfferSpec("IDR", DirectionEnum.BUY, 1_000_000, "tara", "Tara", "tara_id", fiatPaymentMethod = "BANK_TRANSFER", reputationTotalScore = 41_000, ranking = 39),
            OfferSpec("IDR", DirectionEnum.SELL, 5_000_000, "umar", "Umar", "umar_btc", fiatPaymentMethod = "CASH_DEPOSIT", reputationTotalScore = 76_000, ranking = 15),
            // PYG (high-inflation, lower-volume)
            OfferSpec("PYG", DirectionEnum.BUY, 500_000, "vera", "Vera", "vera_py", fiatPaymentMethod = "BANK_TRANSFER", reputationTotalScore = 28_000, ranking = 64, daysAgo = 6),
            // LBP (high-inflation)
            OfferSpec("LBP", DirectionEnum.SELL, 50_000_000, "wassim", "Wassim", "wassim_lb", fiatPaymentMethod = "CASH_DEPOSIT", reputationTotalScore = 14_000, ranking = 102, daysAgo = 7),
        )

    private val nowMillis = 1_741_912_747_000L
    private val oneDayMillis = 86_400_000L

    val offers: List<OfferItemPresentationDto> =
        offerSpecs.mapIndexed { idx, spec -> buildDemoOffer(idx, spec) }

    private fun buildDemoOffer(
        idx: Int,
        spec: OfferSpec,
    ): OfferItemPresentationDto {
        val market = MarketVO("BTC", spec.quote)
        val priceValue =
            pricePerQuoteCurrency[spec.quote]
                ?: error("No demo price configured for ${spec.quote}")
        val priceQuote =
            PriceQuoteVO(
                priceValue,
                4,
                2,
                market,
                CoinVO("BTC", 1, "BTC", 8, 4),
                FiatVO(spec.quote, priceValue, spec.quote, 4, 2),
            )
        val offerDate = nowMillis - (spec.daysAgo * oneDayMillis)
        val makerKey = spec.makerKey
        // The maker IS the user-profile owner, so makerNetworkId.pubKey must match
        // userProfile.networkId.pubKey exactly (real protocol invariant). Build once
        // and reuse to prevent drift.
        val makerPubKey =
            PubKeyVO(
                publicKey = PublicKeyVO(encoded = "$makerKey-pub"),
                keyId = "$makerKey-keyid",
                hash = "$makerKey-hash",
                id = makerKey,
            )
        val makerNetworkId =
            NetworkIdVO(
                addressByTransportTypeMap = AddressByTransportTypeMapVO(map = mapOf()),
                pubKey = makerPubKey,
            )

        return OfferItemPresentationDto(
            bisqEasyOffer =
                BisqEasyOfferVO(
                    id = "demo-offer-$idx",
                    date = offerDate,
                    makerNetworkId = makerNetworkId,
                    direction = spec.direction,
                    market = market,
                    amountSpec = QuoteSideFixedAmountSpecVO(amount = spec.fiatAmount),
                    priceSpec = FixPriceSpecVO(priceQuote = priceQuote),
                    protocolTypes = listOf(),
                    baseSidePaymentMethodSpecs =
                        listOf(
                            BitcoinPaymentMethodSpecVO(
                                paymentMethod = spec.bitcoinPaymentMethod,
                                saltedMakerAccountId = "$makerKey-btc",
                            ),
                        ),
                    quoteSidePaymentMethodSpecs =
                        listOf(
                            FiatPaymentMethodSpecVO(
                                paymentMethod = spec.fiatPaymentMethod,
                                saltedMakerAccountId = "$makerKey-fiat",
                            ),
                        ),
                    offerOptions = listOf(),
                    supportedLanguageCodes = listOf("EN"),
                ),
            isMyOffer = spec.isMyOffer,
            userProfile =
                UserProfileVO(
                    version = 1,
                    nickName = spec.makerNickName,
                    proofOfWork =
                        ProofOfWorkVO(
                            payloadEncoded = "$makerKey-payload",
                            counter = 1L,
                            challengeEncoded = "$makerKey-challenge",
                            difficulty = 2.0,
                            solutionEncoded = "$makerKey-solution",
                            duration = 2000L,
                        ),
                    avatarVersion = 0,
                    networkId = makerNetworkId,
                    terms = "",
                    statement = "",
                    applicationVersion = "",
                    nym = "$makerKey-nym",
                    userName = spec.makerUserName,
                    publishDate = offerDate,
                ),
            formattedDate = "",
            formattedQuoteAmount = "",
            formattedBaseAmount = "",
            formattedPrice = "",
            formattedPriceSpec = "",
            // baseSide = BTC delivery method, quoteSide = fiat delivery method.
            // The original hardcoded demo offers had these swapped; preserved here as
            // the correct mapping while keeping the existing field naming.
            baseSidePaymentMethods = listOf(spec.bitcoinPaymentMethod),
            quoteSidePaymentMethods = listOf(spec.fiatPaymentMethod),
            reputationScore =
                ReputationScoreVO(
                    totalScore = spec.reputationTotalScore,
                    fiveSystemScore = (1.0 + (spec.reputationTotalScore / 25_000.0)).coerceAtMost(5.0),
                    ranking = spec.ranking,
                ),
        )
    }

    val numOffers = offers.groupingBy { it.bisqEasyOffer.market.quoteCurrencyCode }.eachCount()

    // NUM_USER_PROFILES payload is a single Int (network-wide count). Used by the
    // bootstrap "initial subscriptions received data" gate; the value is also surfaced
    // as a network-activity hint in some screens.
    const val NUM_USER_PROFILES: Int = 187
}
