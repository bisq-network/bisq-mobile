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
import network.bisq.mobile.data.model.account.PaymentAccountDto
import network.bisq.mobile.data.model.account.fiat.UserDefinedFiatAccountDto
import network.bisq.mobile.data.model.account.fiat.UserDefinedFiatAccountPayloadDto
import network.bisq.mobile.data.model.account.fiat.ZelleAccountDto
import network.bisq.mobile.data.model.account.fiat.ZelleAccountPayloadDto
import network.bisq.mobile.data.model.trade.ClosedTradeListItemDto
import network.bisq.mobile.data.replicated.chat.ChatMessageTypeEnum
import network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelDto
import network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageDto
import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.replicated.common.currency.marketListDemoObj
import network.bisq.mobile.data.replicated.common.monetary.CoinVO
import network.bisq.mobile.data.replicated.common.monetary.FiatVO
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.data.replicated.common.network.AddressByTransportTypeMapVO
import network.bisq.mobile.data.replicated.contract.BisqEasyContractVO
import network.bisq.mobile.data.replicated.contract.PartyVO
import network.bisq.mobile.data.replicated.contract.RoleEnum
import network.bisq.mobile.data.replicated.identity.IdentityVO
import network.bisq.mobile.data.replicated.identity.identitiesDemoObj
import network.bisq.mobile.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.data.replicated.offer.amount.spec.QuoteSideFixedAmountSpecVO
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.data.replicated.offer.payment_method.BitcoinPaymentMethodSpecVO
import network.bisq.mobile.data.replicated.offer.payment_method.FiatPaymentMethodSpecVO
import network.bisq.mobile.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationDto
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationDto
import network.bisq.mobile.data.replicated.security.keys.I2pKeyPairVO
import network.bisq.mobile.data.replicated.security.keys.KeyBundleVO
import network.bisq.mobile.data.replicated.security.keys.KeyPairVO
import network.bisq.mobile.data.replicated.security.keys.PrivateKeyVO
import network.bisq.mobile.data.replicated.security.keys.PubKeyVO
import network.bisq.mobile.data.replicated.security.keys.PublicKeyVO
import network.bisq.mobile.data.replicated.security.keys.TorKeyPairVO
import network.bisq.mobile.data.replicated.security.pow.ProofOfWorkVO
import network.bisq.mobile.data.replicated.settings.apiVersionSettingsVO
import network.bisq.mobile.data.replicated.settings.settingsVODemoObj
import network.bisq.mobile.data.replicated.trade.TradeRoleEnum
import network.bisq.mobile.data.replicated.trade.bisq_easy.BisqEasyTradeDto
import network.bisq.mobile.data.replicated.trade.bisq_easy.BisqEasyTradePartyVO
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.data.replicated.user.identity.UserIdentityVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.userProfileDemoObj
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.core.pagination.PaginatedResponse
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.domain.utils.createUuid

class WebSocketClientDemo(
    private val json: Json,
) : WebSocketClient,
    Logging {
    override val apiUrl = parseUrl("http://demo.bisq:21")!!

    private val emptyClosedTradesPage =
        PaginatedResponse<ClosedTradeListItemDto>(
            items = emptyList(),
            page = 1,
            pageSize = 1,
            totalItems = 0,
            totalPages = 0,
        )

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

                // Payment accounts - returns List<FiatAccount>. Seeded so the
                // Settings → Payment Accounts screen is populated for App Review.
                webSocketRequest.path.contains("payment-accounts/fiat") ->
                    json.encodeToString(FakeSubscriptionData.paymentAccounts)

                // Trades - closed trades paginated REST endpoint. Demo returns an
                // empty page so (a) the BackendCapabilitiesService probe succeeds
                // and (b) the closed-trades tab renders without parse errors.
                webSocketRequest.path.contains("trades/closed") ->
                    json.encodeToString(emptyClosedTradesPage)

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
            Topic.TRADES -> json.encodeToString(FakeSubscriptionData.trades)
            // No incremental properties needed: each demo trade carries its own initial
            // tradeState inside BisqEasyTradeDto. The subscription must still emit so the
            // consumer's collect() loop is satisfied.
            Topic.TRADE_PROPERTIES -> "[]"
            Topic.TRADE_CHAT_MESSAGES -> json.encodeToString(FakeSubscriptionData.chatMessages)
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

    // ---------------------------------------------------------------------------
    // Trades, chat, and payment accounts (seeded for App Store Review demo video).
    //
    // The demo user (userProfileDemoObj) is the taker in all three trades. Makers
    // are reused from offerSpecs so avatars/reputation already render correctly.
    // ---------------------------------------------------------------------------

    private data class TradeSpec(
        val makerKey: String, // mirrors an OfferSpec maker for consistent avatars
        val makerNickName: String,
        val makerUserName: String,
        val quote: String,
        val fiatAmount: Long, // minor units (e.g. cents)
        val baseSideSats: Long, // BTC amount in sats
        val fiatPaymentMethod: String,
        val bitcoinPaymentMethod: String = "MAIN_CHAIN",
        val makerReputationTotalScore: Long,
        val makerReputationRanking: Int,
        val daysAgo: Int,
        val tradeRole: TradeRoleEnum, // demo user's role
        val direction: DirectionEnum, // maker's direction
        val tradeState: BisqEasyTradeStateEnum,
        val tradeCompletedDate: Long? = null,
        val paymentAccountData: String? = null, // seller-provided fiat account info shown to buyer
        val bitcoinPaymentData: String? = null, // buyer-provided BTC address shown to seller
    )

    // The demo user's identity. networkId.pubKey MUST match userProfileDemoObj's
    // pubKey so the contract's taker party + the trade's myIdentity are consistent.
    //
    // Declared before `trades` because Kotlin object properties init in source order
    // and buildDemoTrade() reads this field.
    private val demoMyIdentity: IdentityVO =
        IdentityVO(
            tag = "demo-identity",
            networkId = userProfileDemoObj.networkId,
            keyBundle =
                KeyBundleVO(
                    keyId = "demo-keyid",
                    keyPair =
                        KeyPairVO(
                            publicKey = PublicKeyVO(encoded = "demo-pub"),
                            privateKey = PrivateKeyVO(encoded = "demo-priv"),
                        ),
                    torKeyPair =
                        TorKeyPairVO(
                            privateKeyEncoded = "demo-tor-priv",
                            publicKeyEncoded = "demo-tor-pub",
                            onionAddress = "demo.onion",
                        ),
                    i2pKeyPair =
                        I2pKeyPairVO(
                            identityBytes = "demo-i2p-id",
                            destinationBytes = "demo-i2p-dest",
                        ),
                ),
        )

    // 3 trades covering the lifecycle: action-needed, in-flight, completed.
    private val tradeSpecs: List<TradeSpec> =
        listOf(
            // Trade 1 — buyer (demo user) needs to send fiat. Action card on My Trades.
            TradeSpec(
                makerKey = "alice",
                makerNickName = "Alice",
                makerUserName = "alice_btc",
                quote = "USD",
                fiatAmount = 10_000L, // $100.00 — minor units (×100)
                baseSideSats = 105_000L, // ~0.00105 BTC at ≈$95k
                fiatPaymentMethod = "ZELLE",
                makerReputationTotalScore = 91_000,
                makerReputationRanking = 8,
                daysAgo = 0,
                tradeRole = TradeRoleEnum.BUYER_AS_TAKER,
                direction = DirectionEnum.SELL,
                tradeState =
                    BisqEasyTradeStateEnum
                        .TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
                paymentAccountData = "Zelle: alice@example.com",
                bitcoinPaymentData = "bc1qdemo000buyeraddressplaceholder000xxxxx",
            ),
            // Trade 2 — buyer has sent fiat, awaiting seller to release BTC. Shows
            // peer-action state.
            TradeSpec(
                makerKey = "bob",
                makerNickName = "Bob",
                makerUserName = "bobsly",
                quote = "EUR",
                fiatAmount = 25_000L, // €250.00
                baseSideSats = 285_000L,
                fiatPaymentMethod = "REVOLUT",
                makerReputationTotalScore = 56_000,
                makerReputationRanking = 24,
                daysAgo = 1,
                tradeRole = TradeRoleEnum.BUYER_AS_TAKER,
                direction = DirectionEnum.SELL,
                tradeState = BisqEasyTradeStateEnum.BUYER_SENT_FIAT_SENT_CONFIRMATION,
                paymentAccountData = "Revolut: @bobsly",
                bitcoinPaymentData = "bc1qdemo000buyeraddressplaceholder000xxxxx",
            ),
            // Trade 3 — fully completed.
            TradeSpec(
                makerKey = "elena",
                makerNickName = "Elena",
                makerUserName = "elena_eu",
                quote = "EUR",
                fiatAmount = 20_000L,
                baseSideSats = 227_000L,
                fiatPaymentMethod = "SEPA",
                makerReputationTotalScore = 78_000,
                makerReputationRanking = 14,
                daysAgo = 5,
                tradeRole = TradeRoleEnum.BUYER_AS_TAKER,
                direction = DirectionEnum.SELL,
                tradeState = BisqEasyTradeStateEnum.BTC_CONFIRMED,
                tradeCompletedDate = nowMillis - 4 * oneDayMillis,
                paymentAccountData = "SEPA: DE89 3704 0044 0532 0130 00 / BIC COBADEFFXXX",
                bitcoinPaymentData = "bc1qdemo000buyeraddressplaceholder000xxxxx",
            ),
        )

    val trades: List<TradeItemPresentationDto> =
        tradeSpecs.mapIndexed { idx, spec -> buildDemoTrade(idx, spec) }

    // Five-message exchange seeded on Trade 1 so opening it from My Trades shows
    // a populated chat (App Review feedback expected the chat surface to be visible).
    val chatMessages: List<BisqEasyOpenTradeMessageDto> = buildDemoChatMessagesForTrade1()

    val paymentAccounts: List<PaymentAccountDto> =
        listOf(
            ZelleAccountDto(
                accountName = "My Zelle",
                accountPayload =
                    ZelleAccountPayloadDto(
                        holderName = "Satoshi Demo",
                        emailOrMobileNr = "demo@example.com",
                        paymentMethodName = "Zelle",
                        currency = "USD",
                        country = "US",
                    ),
            ),
            UserDefinedFiatAccountDto(
                accountName = "Bank transfer (notes)",
                accountPayload =
                    UserDefinedFiatAccountPayloadDto(
                        accountData =
                            "IBAN: DE89 3704 0044 0532 0130 00\n" +
                                "BIC: COBADEFFXXX\n" +
                                "Holder: Satoshi Demo",
                        paymentMethodName = "Custom",
                    ),
            ),
        )

    private fun buildDemoTrade(
        idx: Int,
        spec: TradeSpec,
    ): TradeItemPresentationDto {
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
        val makerPubKey =
            PubKeyVO(
                publicKey = PublicKeyVO(encoded = "${spec.makerKey}-pub"),
                keyId = "${spec.makerKey}-keyid",
                hash = "${spec.makerKey}-hash",
                id = spec.makerKey,
            )
        val makerNetworkId =
            NetworkIdVO(
                addressByTransportTypeMap = AddressByTransportTypeMapVO(map = mapOf()),
                pubKey = makerPubKey,
            )
        val makerUserProfile =
            UserProfileVO(
                version = 1,
                nickName = spec.makerNickName,
                proofOfWork =
                    ProofOfWorkVO(
                        payloadEncoded = "${spec.makerKey}-payload",
                        counter = 1L,
                        challengeEncoded = "${spec.makerKey}-challenge",
                        difficulty = 2.0,
                        solutionEncoded = "${spec.makerKey}-solution",
                        duration = 2000L,
                    ),
                avatarVersion = 0,
                networkId = makerNetworkId,
                terms = "",
                statement = "",
                applicationVersion = "",
                nym = "${spec.makerKey}-nym",
                userName = spec.makerUserName,
                publishDate = offerDate,
            )

        // Taker = the demo user (mirrors userProfileDemoObj exactly for the pubKey
        // invariant: taker.networkId.pubKey must equal myIdentity.networkId.pubKey).
        val takerUserProfile = userProfileDemoObj
        val takerNetworkId = takerUserProfile.networkId

        val offer =
            BisqEasyOfferVO(
                id = "demo-trade-offer-$idx",
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
                            saltedMakerAccountId = "${spec.makerKey}-btc",
                        ),
                    ),
                quoteSidePaymentMethodSpecs =
                    listOf(
                        FiatPaymentMethodSpecVO(
                            paymentMethod = spec.fiatPaymentMethod,
                            saltedMakerAccountId = "${spec.makerKey}-fiat",
                        ),
                    ),
                offerOptions = listOf(),
                supportedLanguageCodes = listOf("EN"),
            )

        val contract =
            BisqEasyContractVO(
                takeOfferDate = offerDate,
                offer = offer,
                maker = PartyVO(role = RoleEnum.MAKER, networkId = makerNetworkId),
                taker = PartyVO(role = RoleEnum.TAKER, networkId = takerNetworkId),
                baseSideAmount = spec.baseSideSats,
                quoteSideAmount = spec.fiatAmount,
                baseSidePaymentMethodSpec =
                    BitcoinPaymentMethodSpecVO(
                        paymentMethod = spec.bitcoinPaymentMethod,
                        saltedMakerAccountId = "${spec.makerKey}-btc",
                    ),
                quoteSidePaymentMethodSpec =
                    FiatPaymentMethodSpecVO(
                        paymentMethod = spec.fiatPaymentMethod,
                        saltedMakerAccountId = "${spec.makerKey}-fiat",
                    ),
                mediator = null,
                priceSpec = FixPriceSpecVO(priceQuote = priceQuote),
                marketPrice = priceValue,
            )

        val tradeId = "demo-trade-$idx"
        val trade =
            BisqEasyTradeDto(
                contract = contract,
                id = tradeId,
                tradeRole = spec.tradeRole,
                myIdentity = demoMyIdentity,
                taker = BisqEasyTradePartyVO(networkId = takerNetworkId),
                maker = BisqEasyTradePartyVO(networkId = makerNetworkId),
                tradeState = spec.tradeState,
                paymentAccountData = spec.paymentAccountData,
                bitcoinPaymentData = spec.bitcoinPaymentData,
                paymentProof = null,
                interruptTradeInitiator = null,
                errorMessage = null,
                errorStackTrace = null,
                peersErrorMessage = null,
                peersErrorStackTrace = null,
                tradeCompletedDate = spec.tradeCompletedDate,
            )

        val channel =
            BisqEasyOpenTradeChannelDto(
                id = "demo-channel-$idx",
                tradeId = tradeId,
                bisqEasyOffer = offer,
                myUserIdentity = UserIdentityVO(identity = demoMyIdentity, userProfile = takerUserProfile),
                traders = setOf(makerUserProfile, takerUserProfile),
                mediator = null,
            )

        val directionLabel = if (spec.tradeRole.isBuyer) "Buy" else "Sell"
        val baseFormatted = "${spec.baseSideSats / 100_000_000.0} BTC"
        val quoteFormatted = "${spec.fiatAmount / 100.0} ${spec.quote}"
        // priceValue is stored in 2-decimal minor units (consistent with `fiatAmount` in
        // TradeSpec): 9_500_000 = 95,000.00 — match that scale here so the formatted
        // string matches the comments on `pricePerQuoteCurrency` ("≈ 95,000 USD/BTC").
        val priceFormatted = "${priceValue / 100} ${spec.quote}"

        return TradeItemPresentationDto(
            channel = channel,
            trade = trade,
            makerUserProfile = makerUserProfile,
            takerUserProfile = takerUserProfile,
            mediatorUserProfile = null,
            directionalTitle = "$directionLabel Bitcoin",
            formattedDate = "${spec.daysAgo}d ago",
            formattedTime = "",
            market = "BTC/${spec.quote}",
            price = priceValue,
            formattedPrice = priceFormatted,
            baseAmount = spec.baseSideSats,
            formattedBaseAmount = baseFormatted,
            quoteAmount = spec.fiatAmount,
            formattedQuoteAmount = quoteFormatted,
            bitcoinSettlementMethod = spec.bitcoinPaymentMethod,
            bitcoinSettlementMethodDisplayString = spec.bitcoinPaymentMethod,
            fiatPaymentMethod = spec.fiatPaymentMethod,
            fiatPaymentMethodDisplayString = spec.fiatPaymentMethod,
            isFiatPaymentMethodCustom = false,
            formattedMyRole = if (spec.tradeRole.isBuyer) "Buyer (Taker)" else "Seller (Taker)",
            peersReputationScore =
                ReputationScoreVO(
                    totalScore = spec.makerReputationTotalScore,
                    fiveSystemScore =
                        (1.0 + (spec.makerReputationTotalScore / 25_000.0)).coerceAtMost(5.0),
                    ranking = spec.makerReputationRanking,
                ),
        )
    }

    private fun buildDemoChatMessagesForTrade1(): List<BisqEasyOpenTradeMessageDto> {
        val firstTrade = trades.firstOrNull() ?: return emptyList()
        val tradeId = firstTrade.trade.id
        val channelId = firstTrade.channel.id
        val maker = firstTrade.makerUserProfile
        val taker = firstTrade.takerUserProfile
        val baseTime = nowMillis - oneDayMillis / 24 // start 1 hour ago
        val minute = 60_000L

        // Five-message exchange. Alternates seller (maker) and buyer (taker = demo
        // user) so the chat surface visibly shows both bubbles.
        return listOf(
            chatMessage(
                tradeId = tradeId,
                channelId = channelId,
                idx = 0,
                sender = maker,
                receiver = taker,
                text = "Hi! Trade taken. I'll send my Zelle details shortly.",
                date = baseTime,
            ),
            chatMessage(
                tradeId = tradeId,
                channelId = channelId,
                idx = 1,
                sender = maker,
                receiver = taker,
                text = "Account: alice@example.com — please include your trade ID in the memo.",
                date = baseTime + 2 * minute,
            ),
            chatMessage(
                tradeId = tradeId,
                channelId = channelId,
                idx = 2,
                sender = taker,
                receiver = maker,
                text = "Thanks! Sending the payment now from my bank app.",
                date = baseTime + 5 * minute,
            ),
            chatMessage(
                tradeId = tradeId,
                channelId = channelId,
                idx = 3,
                sender = taker,
                receiver = maker,
                text = "Payment is on its way. Should arrive within a few minutes.",
                date = baseTime + 6 * minute,
            ),
            chatMessage(
                tradeId = tradeId,
                channelId = channelId,
                idx = 4,
                sender = maker,
                receiver = taker,
                text = "Great, I'll confirm receipt and release the BTC as soon as I see it.",
                date = baseTime + 7 * minute,
            ),
        )
    }

    private fun chatMessage(
        tradeId: String,
        channelId: String,
        idx: Int,
        sender: UserProfileVO,
        receiver: UserProfileVO,
        text: String,
        date: Long,
    ): BisqEasyOpenTradeMessageDto =
        BisqEasyOpenTradeMessageDto(
            tradeId = tradeId,
            messageId = "demo-chat-$tradeId-$idx",
            channelId = channelId,
            senderUserProfile = sender,
            receiverUserProfileId = receiver.networkId.pubKey.id,
            receiverNetworkId = receiver.networkId,
            text = text,
            citation = null,
            date = date,
            mediator = null,
            chatMessageType = ChatMessageTypeEnum.TEXT,
            bisqEasyOffer = null,
            chatMessageReactions = emptySet(),
            citationAuthorUserProfile = null,
        )
}
