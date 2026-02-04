package network.bisq.mobile.client.common.domain.service.push_notification

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeviceRegistrationRequestTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `DeviceRegistrationRequest serializes correctly`() {
        // Given
        val request =
            DeviceRegistrationRequest(
                deviceId = "test-device-id",
                deviceToken = "test-token-abc123",
                publicKeyBase64 = "dGVzdC1wdWJsaWMta2V5",
                deviceDescriptor = "Test Device, Android 14",
                platform = Platform.ANDROID,
            )

        // When
        val serialized = json.encodeToString(request)

        // Then
        assertTrue(serialized.contains("test-device-id"))
        assertTrue(serialized.contains("test-token-abc123"))
        assertTrue(serialized.contains("dGVzdC1wdWJsaWMta2V5"))
        assertTrue(serialized.contains("Test Device, Android 14"))
        assertTrue(serialized.contains("ANDROID"))
    }

    @Test
    fun `DeviceRegistrationRequest deserializes correctly`() {
        // Given
        val jsonString =
            """
            {
                "deviceId": "device-123",
                "deviceToken": "token-456",
                "publicKeyBase64": "cHVibGljS2V5",
                "deviceDescriptor": "Pixel 8, Android 14",
                "platform": "ANDROID"
            }
            """.trimIndent()

        // When
        val request = json.decodeFromString<DeviceRegistrationRequest>(jsonString)

        // Then
        assertEquals("device-123", request.deviceId)
        assertEquals("token-456", request.deviceToken)
        assertEquals("cHVibGljS2V5", request.publicKeyBase64)
        assertEquals("Pixel 8, Android 14", request.deviceDescriptor)
        assertEquals(Platform.ANDROID, request.platform)
    }

    @Test
    fun `DeviceRegistrationRequest round-trip serialization`() {
        // Given
        val original =
            DeviceRegistrationRequest(
                deviceId = "round-trip-id",
                deviceToken = "round-trip-token",
                publicKeyBase64 = "cm91bmQtdHJpcC1rZXk=",
                deviceDescriptor = "iPhone 15 Pro, iOS 17.2",
                platform = Platform.IOS,
            )

        // When
        val serialized = json.encodeToString(original)
        val deserialized = json.decodeFromString<DeviceRegistrationRequest>(serialized)

        // Then
        assertEquals(original, deserialized)
    }

    @Test
    fun `Platform enum serializes correctly`() {
        // Given
        val iosPlatform = Platform.IOS
        val androidPlatform = Platform.ANDROID

        // When / Then
        assertEquals("\"IOS\"", json.encodeToString(iosPlatform))
        assertEquals("\"ANDROID\"", json.encodeToString(androidPlatform))
    }

    @Test
    fun `Platform enum deserializes correctly`() {
        // Given
        val iosJson = "\"IOS\""
        val androidJson = "\"ANDROID\""

        // When / Then
        assertEquals(Platform.IOS, json.decodeFromString(iosJson))
        assertEquals(Platform.ANDROID, json.decodeFromString(androidJson))
    }

    @Test
    fun `DeviceRegistrationRequest data class equality`() {
        // Given
        val request1 =
            DeviceRegistrationRequest(
                deviceId = "id",
                deviceToken = "token",
                publicKeyBase64 = "key",
                deviceDescriptor = "desc",
                platform = Platform.ANDROID,
            )
        val request2 =
            DeviceRegistrationRequest(
                deviceId = "id",
                deviceToken = "token",
                publicKeyBase64 = "key",
                deviceDescriptor = "desc",
                platform = Platform.ANDROID,
            )

        // When / Then
        assertEquals(request1, request2)
        assertEquals(request1.hashCode(), request2.hashCode())
    }
}
