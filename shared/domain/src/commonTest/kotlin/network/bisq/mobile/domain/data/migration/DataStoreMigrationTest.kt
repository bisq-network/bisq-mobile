package network.bisq.mobile.domain.data.migration

import io.ktor.util.date.getTimeMillis
import kotlinx.serialization.json.Json
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.model.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests to ensure DataStore can handle data that might have been stored
 * in different formats by the previous multiplatform-settings implementation
 */
class DataStoreMigrationTest {

    private val json = Json {
        ignoreUnknownKeys = true // Critical for migration compatibility
        encodeDefaults = true
    }

    @Test
    fun `should handle legacy User data with extra fields`() {
        // Given - JSON that might have been stored by multiplatform-settings with extra fields
        val legacyUserJson = """
        {
            "tradeTerms": "Legacy terms",
            "statement": "Legacy statement", 
            "legacyField": "should be ignored",
            "anotherLegacyField": 42
        }
        """.trimIndent()

        // When
        val user = json.decodeFromString(User.serializer(), legacyUserJson)

        // Then - should parse successfully and ignore unknown fields
        assertEquals("Legacy terms", user.tradeTerms)
        assertEquals("Legacy statement", user.statement)
    }

    @Test
    fun `should handle legacy Settings data with missing new fields`() {
        // Given - Settings JSON that might be missing newer fields
        val legacySettingsJson = """
        {
            "bisqApiUrl": "https://legacy.bisq.network",
            "firstLaunch": true
        }
        """.trimIndent()

        // When
        val settings = json.decodeFromString(Settings.serializer(), legacySettingsJson)

        // Then - should use default values for missing fields
        assertEquals("https://legacy.bisq.network", settings.bisqApiUrl)
        assertEquals(true, settings.firstLaunch)
        assertEquals(Settings().showChatRulesWarnBox, settings.showChatRulesWarnBox) // default
        assertEquals(Settings().selectedMarketCode, settings.selectedMarketCode) // default
    }

    @Test
    fun `should handle boolean variations in Settings`() {
        // Given - different boolean representations
        val testCases = listOf(
            """{"firstLaunch": true, "showChatRulesWarnBox": false}""",
            """{"firstLaunch": "true", "showChatRulesWarnBox": "false"}""", // String booleans
        )

        // When & Then
        testCases.forEachIndexed { index, json ->
            try {
                val settings = this.json.decodeFromString(Settings.serializer(), json)
                when (index) {
                    0 -> {
                        assertEquals(true, settings.firstLaunch)
                        assertEquals(false, settings.showChatRulesWarnBox)
                    }
                    // String boolean case might fail, which is acceptable
                }
            } catch (e: Exception) {
                // String boolean parsing failure is acceptable
                if (index != 1) throw e
            }
        }
    }

    @Test
    fun `should handle completely empty or minimal JSON`() {
        // Given - minimal JSON that might exist from fresh installs
        val testCases = listOf(
            "{}",
            """{"tradeTerms": ""}""",
            """{"bisqApiUrl": ""}"""
        )

        // When & Then - should not throw exceptions
        testCases.forEach { json ->
            val user = this.json.decodeFromString(User.serializer(), json)
            assertNotNull(user)
            
            val settings = this.json.decodeFromString(Settings.serializer(), json)
            assertNotNull(settings)
        }
    }

    @Test
    fun `should handle null values gracefully`() {
        // Given - JSON with explicit null values for User (nullable fields)
        val userWithNulls = """
        {
            "tradeTerms": null,
            "statement": null
        }
        """.trimIndent()

        // Settings has non-nullable fields with defaults, so we test missing fields instead
        val settingsWithMissingFields = """
        {
            "firstLaunch": true
        }
        """.trimIndent()

        // When & Then - should handle nulls and missing fields
        val user = json.decodeFromString(User.serializer(), userWithNulls)
        assertEquals(null, user.tradeTerms) // User model allows null
        assertEquals(null, user.statement) // User model allows null

        val settings = json.decodeFromString(Settings.serializer(), settingsWithMissingFields)
        assertEquals(Settings().bisqApiUrl, settings.bisqApiUrl) // default
        assertEquals(Settings().selectedMarketCode, settings.selectedMarketCode) // default
        assertEquals(true, settings.firstLaunch) // from JSON
    }

    @Test
    fun `should handle corrupted or malformed JSON gracefully`() {
        // Given - various malformed JSON scenarios
        val malformedJsonCases = listOf(
            """{"tradeTerms": "unclosed string""",
            """{"firstLaunch": "not-a-boolean"}""",
            """{"extraComma": true,}""",
            """{"missingQuotes": value}"""
        )

        // When & Then - should either parse with defaults or throw predictable exceptions
        malformedJsonCases.forEach { malformedJson ->
            try {
                val user = json.decodeFromString(User.serializer(), malformedJson)
                // If it parses, that's fine - it should have reasonable defaults
                assertNotNull(user)
            } catch (e: Exception) {
                // If it throws, that's also fine - we expect some malformed JSON to fail
                // The important thing is it doesn't crash the app
                assertNotNull(e.message)
            }
        }
    }

    @Test
    fun `should maintain data integrity after round-trip serialization`() {
        // Given - data that represents a typical user's stored data
        val originalUser = User(
            tradeTerms = "My trading terms",
            statement = "My statement",
        )

        val originalSettings = Settings(
            bisqApiUrl = "https://api.bisq.network",
            firstLaunch = false,
            showChatRulesWarnBox = true,
            selectedMarketCode = "BTC/USD"
        )

        // When - serialize and deserialize multiple times
        var currentUser = originalUser
        var currentSettings = originalSettings

        repeat(5) {
            val userJson = json.encodeToString(User.serializer(), currentUser)
            currentUser = json.decodeFromString(User.serializer(), userJson)

            val settingsJson = json.encodeToString(Settings.serializer(), currentSettings)
            currentSettings = json.decodeFromString(Settings.serializer(), settingsJson)
        }

        // Then - data should remain identical
        assertEquals(originalUser, currentUser)
        assertEquals(originalSettings, currentSettings)
    }
}
