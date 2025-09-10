package network.bisq.mobile.domain.data.serialization

import kotlinx.serialization.json.Json
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.model.TradeReadStateMap
import network.bisq.mobile.domain.data.model.User
import kotlin.test.Test
import kotlin.test.assertEquals

class DataStoreSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `User serialization should be stable and backwards compatible`() {
        // Given
        val user = User(
            tradeTerms = "Test terms with special chars: àáâãäåæçèéêë",
            statement = "Test statement with emojis: 🚀💰📈",
        )

        // When - serialize and deserialize
        val serialized = json.encodeToString(User.serializer(), user)
        val deserialized = json.decodeFromString(User.serializer(), serialized)

        // Then
        assertEquals(user, deserialized)
        assertEquals(user.tradeTerms, deserialized.tradeTerms)
        assertEquals(user.statement, deserialized.statement)
    }

    @Test
    fun `User deserialization should handle missing fields gracefully`() {
        // Given - JSON with missing optional fields
        val jsonWithMissingFields = """{"tradeTerms":"test"}"""

        // When
        val deserialized = json.decodeFromString(User.serializer(), jsonWithMissingFields)

        // Then - should use default values (null for User model)
        assertEquals("test", deserialized.tradeTerms)
        assertEquals(null, deserialized.statement) // User model defaults to null
    }

    @Test
    fun `Settings serialization should preserve all fields`() {
        // Given
        val settings = Settings(
            bisqApiUrl = "https://api.bisq.network/test",
            firstLaunch = false,
            showChatRulesWarnBox = true,
            selectedMarketCode = "BTC/EUR"
        )

        // When
        val serialized = json.encodeToString(Settings.serializer(), settings)
        val deserialized = json.decodeFromString(Settings.serializer(), serialized)

        // Then
        assertEquals(settings, deserialized)
        assertEquals(settings.bisqApiUrl, deserialized.bisqApiUrl)
        assertEquals(settings.firstLaunch, deserialized.firstLaunch)
        assertEquals(settings.showChatRulesWarnBox, deserialized.showChatRulesWarnBox)
        assertEquals(settings.selectedMarketCode, deserialized.selectedMarketCode)
    }

    @Test
    fun `TradeReadStateMap serialization should handle complex maps`() {
        // Given
        val tradeMap = TradeReadStateMap(
            mapOf(
                "trade-with-dashes" to 5,
                "trade_with_underscores" to 10,
                "trade.with.dots" to 0,
                "trade with spaces" to 999,
                "trade-with-unicode-🚀" to 42
            )
        )

        // When
        val serialized = json.encodeToString(TradeReadStateMap.serializer(), tradeMap)
        val deserialized = json.decodeFromString(TradeReadStateMap.serializer(), serialized)

        // Then
        assertEquals(tradeMap, deserialized)
        assertEquals(5, deserialized.map.size)
        assertEquals(5, deserialized.map["trade-with-dashes"])
        assertEquals(42, deserialized.map["trade-with-unicode-🚀"])
    }

    @Test
    fun `Empty data structures should serialize correctly`() {
        // Given
        val emptyUser = User()
        val emptySettings = Settings()
        val emptyTradeMap = TradeReadStateMap(emptyMap())

        // When & Then
        val userSerialized = json.encodeToString(User.serializer(), emptyUser)
        val userDeserialized = json.decodeFromString(User.serializer(), userSerialized)
        assertEquals(emptyUser, userDeserialized)

        val settingsSerialized = json.encodeToString(Settings.serializer(), emptySettings)
        val settingsDeserialized = json.decodeFromString(Settings.serializer(), settingsSerialized)
        assertEquals(emptySettings, settingsDeserialized)

        val tradeSerialized = json.encodeToString(TradeReadStateMap.serializer(), emptyTradeMap)
        val tradeDeserialized = json.decodeFromString(TradeReadStateMap.serializer(), tradeSerialized)
        assertEquals(emptyTradeMap, tradeDeserialized)
    }

    @Test
    fun `Serialization should be deterministic`() {
        // Given
        val user = User(tradeTerms = "test", statement = "statement")

        // When - serialize multiple times
        val serialized1 = json.encodeToString(User.serializer(), user)
        val serialized2 = json.encodeToString(User.serializer(), user)

        // Then - should produce identical results
        assertEquals(serialized1, serialized2)
    }

    @Test
    fun `Large data should serialize without issues`() {
        // Given - large data structures
        val largeStatement = "x".repeat(10000) // 10KB string
        val largeUser = User(
            tradeTerms = "Large terms: $largeStatement",
            statement = largeStatement,
        )

        val largeTradeMap = TradeReadStateMap(
            (1..1000).associate { "trade-$it" to it }
        )

        // When & Then - should not throw
        val userSerialized = json.encodeToString(User.serializer(), largeUser)
        val userDeserialized = json.decodeFromString(User.serializer(), userSerialized)
        assertEquals(largeUser, userDeserialized)

        val tradeSerialized = json.encodeToString(TradeReadStateMap.serializer(), largeTradeMap)
        val tradeDeserialized = json.decodeFromString(TradeReadStateMap.serializer(), tradeSerialized)
        assertEquals(largeTradeMap, tradeDeserialized)
        assertEquals(1000, tradeDeserialized.map.size)
    }
}
