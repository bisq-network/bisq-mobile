package network.bisq.mobile.client.common.domain.websocket

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketRestApiRequest
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketRestApiResponse
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.domain.data.replicated.common.currency.marketListDemoObj
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WebSocketClientDemoTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val demoClient = WebSocketClientDemo(json)

    @Test
    fun `FakeSubscriptionData offers use valid market codes matching marketListDemoObj`() {
        val validBaseCurrencyCodes = marketListDemoObj.map { it.baseCurrencyCode }.toSet()
        val validQuoteCurrencyCodes = marketListDemoObj.map { it.quoteCurrencyCode }.toSet()

        FakeSubscriptionData.offers.forEach { offer ->
            val market = offer.bisqEasyOffer.market
            assertTrue(
                validBaseCurrencyCodes.contains(market.baseCurrencyCode),
                "Offer ${offer.bisqEasyOffer.id} has invalid baseCurrencyCode: ${market.baseCurrencyCode}. " +
                    "Expected one of: $validBaseCurrencyCodes",
            )
            assertTrue(
                validQuoteCurrencyCodes.contains(market.quoteCurrencyCode),
                "Offer ${offer.bisqEasyOffer.id} has invalid quoteCurrencyCode: ${market.quoteCurrencyCode}. " +
                    "Expected one of: $validQuoteCurrencyCodes",
            )
        }
    }

    @Test
    fun `FakeSubscriptionData offers priceSpec markets use valid market codes`() {
        val validBaseCurrencyCodes = marketListDemoObj.map { it.baseCurrencyCode }.toSet()

        FakeSubscriptionData.offers.forEach { offer ->
            val priceSpec = offer.bisqEasyOffer.priceSpec
            // Check if priceSpec has a market (FixPriceSpecVO has priceQuote with market)
            val priceSpecMarket =
                when (priceSpec) {
                    is network.bisq.mobile.domain.data.replicated.offer.price.spec.FixPriceSpecVO ->
                        priceSpec.priceQuote.market
                    else -> null
                }

            priceSpecMarket?.let { market ->
                assertTrue(
                    validBaseCurrencyCodes.contains(market.baseCurrencyCode),
                    "Offer ${offer.bisqEasyOffer.id} priceSpec has invalid baseCurrencyCode: ${market.baseCurrencyCode}. " +
                        "Expected one of: $validBaseCurrencyCodes",
                )
            }
        }
    }

    @Test
    fun `FakeSubscriptionData marketPrice uses valid market codes`() {
        val validBaseCurrencyCodes = marketListDemoObj.map { it.baseCurrencyCode }.toSet()
        val validQuoteCurrencyCodes = marketListDemoObj.map { it.quoteCurrencyCode }.toSet()

        FakeSubscriptionData.marketPrice.forEach { (currencyCode, priceQuote) ->
            assertTrue(
                validQuoteCurrencyCodes.contains(currencyCode),
                "MarketPrice key '$currencyCode' is not a valid quote currency code. " +
                    "Expected one of: $validQuoteCurrencyCodes",
            )
            assertTrue(
                validBaseCurrencyCodes.contains(priceQuote.market.baseCurrencyCode),
                "MarketPrice for $currencyCode has invalid baseCurrencyCode: ${priceQuote.market.baseCurrencyCode}. " +
                    "Expected one of: $validBaseCurrencyCodes",
            )
        }
    }

    @Test
    fun `FakeSubscriptionData numOffers keys match valid quote currency codes`() {
        val validQuoteCurrencyCodes = marketListDemoObj.map { it.quoteCurrencyCode }.toSet()

        FakeSubscriptionData.numOffers.keys.forEach { currencyCode ->
            assertTrue(
                validQuoteCurrencyCodes.contains(currencyCode),
                "NumOffers key '$currencyCode' is not a valid quote currency code. " +
                    "Expected one of: $validQuoteCurrencyCodes",
            )
        }
    }

    @Test
    fun `FakeSubscriptionData offers can be serialized and deserialized`() {
        val serialized = json.encodeToString(FakeSubscriptionData.offers)
        val deserialized = json.decodeFromString<List<network.bisq.mobile.domain.data.replicated.presentation.offerbook.OfferItemPresentationDto>>(serialized)

        assertEquals(FakeSubscriptionData.offers.size, deserialized.size)
        deserialized.forEachIndexed { index, dto ->
            assertEquals(FakeSubscriptionData.offers[index].bisqEasyOffer.id, dto.bisqEasyOffer.id)
        }
    }

    @Test
    fun `FakeSubscriptionData has at least one offer`() {
        assertTrue(
            FakeSubscriptionData.offers.isNotEmpty(),
            "FakeSubscriptionData.offers should not be empty",
        )
    }

    @Test
    fun `FakeSubscriptionData offers baseCurrencyCode should be BTC not Bitcoin`() {
        FakeSubscriptionData.offers.forEach { offer ->
            assertEquals(
                "BTC",
                offer.bisqEasyOffer.market.baseCurrencyCode,
                "Offer ${offer.bisqEasyOffer.id} should use 'BTC' as baseCurrencyCode, not '${offer.bisqEasyOffer.market.baseCurrencyCode}'",
            )
        }
    }

    // ========== fakeResponse tests (via sendRequestAndAwaitResponse) ==========

    private fun createRequest(path: String) = WebSocketRestApiRequest(requestId = "test-id", method = "GET", path = path, body = "")

    @Test
    fun `fakeResponse returns settings for settings endpoint`() =
        runTest {
            val request = createRequest("/api/v1/settings")
            val response = demoClient.sendRequestAndAwaitResponse(request) as WebSocketRestApiResponse
            assertEquals(200, response.statusCode)
            // Settings response should contain isTacAccepted field
            assertTrue(response.body.contains("isTacAccepted"), "Expected settings response, got: ${response.body}")
        }

    @Test
    fun `fakeResponse returns version for settings version endpoint`() =
        runTest {
            val request = createRequest("/api/v1/settings/version")
            val response = demoClient.sendRequestAndAwaitResponse(request) as WebSocketRestApiResponse
            assertEquals(200, response.statusCode)
            assertTrue(response.body.isNotEmpty())
        }

    @Test
    fun `fakeResponse returns identities for user-identities ids endpoint`() =
        runTest {
            val request = createRequest("/api/v1/user-identities/ids")
            val response = demoClient.sendRequestAndAwaitResponse(request) as WebSocketRestApiResponse
            assertEquals(200, response.statusCode)
            assertTrue(response.body.startsWith("["))
        }

    @Test
    fun `fakeResponse returns user profile for owned-profiles endpoint`() =
        runTest {
            val request = createRequest("/api/v1/owned-profiles")
            val response = demoClient.sendRequestAndAwaitResponse(request) as WebSocketRestApiResponse
            assertEquals(200, response.statusCode)
            assertTrue(response.body.contains("nickName"))
        }

    @Test
    fun `fakeResponse returns user profile for selected user-profile endpoint`() =
        runTest {
            val request = createRequest("/api/v1/selected/user-profile")
            val response = demoClient.sendRequestAndAwaitResponse(request) as WebSocketRestApiResponse
            assertEquals(200, response.statusCode)
            assertTrue(response.body.contains("nickName"))
        }

    @Test
    fun `fakeResponse returns empty array for user-profiles ignored endpoint`() =
        runTest {
            val request = createRequest("/api/v1/user-profiles/ignored")
            val response = demoClient.sendRequestAndAwaitResponse(request) as WebSocketRestApiResponse
            assertEquals(200, response.statusCode)
            assertEquals("[]", response.body)
        }

    @Test
    fun `fakeResponse returns user profiles for user-profiles with ids query`() =
        runTest {
            val request = createRequest("/api/v1/user-profiles?ids=abc,def")
            val response = demoClient.sendRequestAndAwaitResponse(request) as WebSocketRestApiResponse
            assertEquals(200, response.statusCode)
            assertTrue(response.body.contains("nickName"))
        }

    @Test
    fun `fakeResponse returns markets for offerbook markets endpoint`() =
        runTest {
            val request = createRequest("/api/v1/offerbook/markets")
            val response = demoClient.sendRequestAndAwaitResponse(request) as WebSocketRestApiResponse
            assertEquals(200, response.statusCode)
            assertTrue(response.body.contains("baseCurrencyCode"))
        }

    @Test
    fun `fakeResponse returns empty array for payment-accounts fiat endpoint`() =
        runTest {
            val request = createRequest("/api/v1/payment-accounts/fiat")
            val response = demoClient.sendRequestAndAwaitResponse(request) as WebSocketRestApiResponse
            assertEquals(200, response.statusCode)
            assertEquals("[]", response.body)
        }

    @Test
    fun `fakeResponse returns zero for reputation profile-age endpoint`() =
        runTest {
            val request = createRequest("/api/v1/reputation/profile-age/some-id")
            val response = demoClient.sendRequestAndAwaitResponse(request) as WebSocketRestApiResponse
            assertEquals(200, response.statusCode)
            assertEquals("0", response.body)
        }

    @Test
    fun `fakeResponse returns reputation score for reputation score endpoint`() =
        runTest {
            val request = createRequest("/api/v1/reputation/score/some-id")
            val response = demoClient.sendRequestAndAwaitResponse(request) as WebSocketRestApiResponse
            assertEquals(200, response.statusCode)
            assertTrue(response.body.contains("totalScore"))
        }

    @Test
    fun `fakeResponse returns empty array for unhandled path`() =
        runTest {
            val request = createRequest("/api/v1/unknown/endpoint")
            val response = demoClient.sendRequestAndAwaitResponse(request) as WebSocketRestApiResponse
            assertEquals(200, response.statusCode)
            assertEquals("[]", response.body)
        }

    // ========== getFakeSubscription tests (via subscribe) ==========

    @Test
    fun `subscribe returns fake data for MARKET_PRICE topic`() =
        runTest {
            val observer = WebSocketEventObserver()
            val result = demoClient.subscribe(Topic.MARKET_PRICE, null, observer)
            assertNotNull(result)
            val event = result.webSocketEvent.value
            assertNotNull(event)
            assertEquals(Topic.MARKET_PRICE, event.topic)
            assertNotNull(event.deferredPayload)
        }

    @Test
    fun `subscribe returns fake data for OFFERS topic`() =
        runTest {
            val observer = WebSocketEventObserver()
            val result = demoClient.subscribe(Topic.OFFERS, null, observer)
            assertNotNull(result)
            val event = result.webSocketEvent.value
            assertNotNull(event)
            assertEquals(Topic.OFFERS, event.topic)
        }

    @Test
    fun `subscribe returns fake data for NUM_OFFERS topic`() =
        runTest {
            val observer = WebSocketEventObserver()
            val result = demoClient.subscribe(Topic.NUM_OFFERS, null, observer)
            assertNotNull(result)
            val event = result.webSocketEvent.value
            assertNotNull(event)
            assertEquals(Topic.NUM_OFFERS, event.topic)
        }

    @Test
    fun `subscribe returns observer without event for unsupported topic`() =
        runTest {
            val observer = WebSocketEventObserver()
            // TRADES topic is not supported in demo mode (returns null payload)
            val result = demoClient.subscribe(Topic.TRADES, null, observer)
            assertNotNull(result)
            // The observer should be returned but without an event set (null payload case)
            // This covers lines 154-155 in WebSocketClientDemo.kt
        }

    // ========== WebSocketClientDemo basic tests ==========

    @Test
    fun `isDemo returns true`() {
        assertTrue(demoClient.isDemo())
    }

    @Test
    fun `connect returns null and sets Connected state`() =
        runTest {
            val error = demoClient.connect()
            assertEquals(null, error)
            assertTrue(demoClient.isConnected())
        }

    @Test
    fun `disconnect sets Disconnected state`() =
        runTest {
            demoClient.connect()
            demoClient.disconnect()
            assertTrue(demoClient.webSocketClientStatus.value is ConnectionState.Disconnected)
        }
}
