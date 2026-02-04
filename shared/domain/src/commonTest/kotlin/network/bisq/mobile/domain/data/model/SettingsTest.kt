package network.bisq.mobile.domain.data.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `Settings has correct default values`() {
        val settings = Settings()

        assertTrue(settings.firstLaunch)
        assertTrue(settings.showChatRulesWarnBox)
        assertEquals("BTC/USD", settings.selectedMarketCode)
        assertEquals(PermissionState.NOT_GRANTED, settings.notificationPermissionState)
        assertEquals(BatteryOptimizationState.NOT_IGNORED, settings.batteryOptimizationState)
        assertFalse(settings.pushNotificationsEnabled)
    }

    @Test
    fun `Settings can be created with custom values`() {
        val settings =
            Settings(
                firstLaunch = false,
                showChatRulesWarnBox = false,
                selectedMarketCode = "BTC/EUR",
                notificationPermissionState = PermissionState.GRANTED,
                batteryOptimizationState = BatteryOptimizationState.IGNORED,
                pushNotificationsEnabled = true,
            )

        assertFalse(settings.firstLaunch)
        assertFalse(settings.showChatRulesWarnBox)
        assertEquals("BTC/EUR", settings.selectedMarketCode)
        assertEquals(PermissionState.GRANTED, settings.notificationPermissionState)
        assertEquals(BatteryOptimizationState.IGNORED, settings.batteryOptimizationState)
        assertTrue(settings.pushNotificationsEnabled)
    }

    @Test
    fun `Settings copy works correctly`() {
        val original = Settings()
        val modified = original.copy(pushNotificationsEnabled = true)

        assertFalse(original.pushNotificationsEnabled)
        assertTrue(modified.pushNotificationsEnabled)
        assertEquals(original.firstLaunch, modified.firstLaunch)
    }

    @Test
    fun `Settings serializes correctly`() {
        val settings = Settings(pushNotificationsEnabled = true)
        val serialized = json.encodeToString(settings)

        assertTrue(serialized.contains("pushNotificationsEnabled"))
        assertTrue(serialized.contains("true"))
    }

    @Test
    fun `Settings deserializes correctly`() {
        val jsonString =
            """
            {
                "firstLaunch": false,
                "showChatRulesWarnBox": true,
                "selectedMarketCode": "BTC/GBP",
                "notificationPermissionState": "GRANTED",
                "batteryOptimizationState": "IGNORED",
                "pushNotificationsEnabled": true
            }
            """.trimIndent()

        val settings = json.decodeFromString<Settings>(jsonString)

        assertFalse(settings.firstLaunch)
        assertTrue(settings.showChatRulesWarnBox)
        assertEquals("BTC/GBP", settings.selectedMarketCode)
        assertEquals(PermissionState.GRANTED, settings.notificationPermissionState)
        assertEquals(BatteryOptimizationState.IGNORED, settings.batteryOptimizationState)
        assertTrue(settings.pushNotificationsEnabled)
    }

    @Test
    fun `PermissionState enum has all expected values`() {
        assertEquals(4, PermissionState.entries.size)
        assertTrue(PermissionState.entries.contains(PermissionState.NOT_GRANTED))
        assertTrue(PermissionState.entries.contains(PermissionState.GRANTED))
        assertTrue(PermissionState.entries.contains(PermissionState.DENIED))
        assertTrue(PermissionState.entries.contains(PermissionState.DONT_ASK_AGAIN))
    }

    @Test
    fun `BatteryOptimizationState enum has all expected values`() {
        assertEquals(3, BatteryOptimizationState.entries.size)
        assertTrue(BatteryOptimizationState.entries.contains(BatteryOptimizationState.NOT_IGNORED))
        assertTrue(BatteryOptimizationState.entries.contains(BatteryOptimizationState.IGNORED))
        assertTrue(BatteryOptimizationState.entries.contains(BatteryOptimizationState.DONT_ASK_AGAIN))
    }

    @Test
    fun `Settings round-trip serialization`() {
        val original =
            Settings(
                firstLaunch = false,
                showChatRulesWarnBox = false,
                selectedMarketCode = "BTC/JPY",
                notificationPermissionState = PermissionState.DENIED,
                batteryOptimizationState = BatteryOptimizationState.DONT_ASK_AGAIN,
                pushNotificationsEnabled = true,
            )

        val serialized = json.encodeToString(original)
        val deserialized = json.decodeFromString<Settings>(serialized)

        assertEquals(original, deserialized)
    }
}
