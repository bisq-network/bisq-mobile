package network.bisq.mobile.client.common.domain.service.push_notification

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for push notification data models and Platform enum.
 * Note: Full integration tests for ClientPushNotificationServiceFacade require
 * mocking infrastructure that is not available in commonTest.
 */
class ClientPushNotificationServiceFacadeTest {
    @Test
    fun `Platform enum has correct values`() {
        assertEquals("IOS", Platform.IOS.name)
        assertEquals("ANDROID", Platform.ANDROID.name)
        assertEquals(2, Platform.entries.size)
    }

    @Test
    fun `DeviceRegistrationRequest contains correct data`() {
        val request =
            DeviceRegistrationRequest(
                userProfileId = "user-profile-123",
                deviceToken = "test-token-12345",
                publicKey = "base64-encoded-public-key",
                platform = Platform.IOS,
            )

        assertEquals("user-profile-123", request.userProfileId)
        assertEquals("test-token-12345", request.deviceToken)
        assertEquals("base64-encoded-public-key", request.publicKey)
        assertEquals(Platform.IOS, request.platform)
    }

    @Test
    fun `DeviceRegistrationRequest for Android platform`() {
        val request =
            DeviceRegistrationRequest(
                userProfileId = "user-profile-456",
                deviceToken = "android-fcm-token",
                publicKey = "base64-encoded-public-key-android",
                platform = Platform.ANDROID,
            )

        assertEquals("user-profile-456", request.userProfileId)
        assertEquals("android-fcm-token", request.deviceToken)
        assertEquals("base64-encoded-public-key-android", request.publicKey)
        assertEquals(Platform.ANDROID, request.platform)
    }

    @Test
    fun `PushNotificationException contains message`() {
        val exception = PushNotificationException("Test error message")
        assertEquals("Test error message", exception.message)
    }

    @Test
    fun `PushNotificationException contains cause`() {
        val cause = RuntimeException("Root cause")
        val exception = PushNotificationException("Test error", cause)

        assertEquals("Test error", exception.message)
        assertEquals(cause, exception.cause)
    }

    // Device Registration Validation Tests

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun `DeviceRegistrationRequest with valid Base64 public key`() {
        val testPublicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE" // Valid Base64
        val request =
            DeviceRegistrationRequest(
                userProfileId = "user-123",
                deviceToken = "device-token-456",
                publicKey = testPublicKey,
                platform = Platform.IOS,
            )

        // Should be able to decode without throwing
        val decoded = Base64.decode(testPublicKey)
        assertNotNull(decoded)
        assertTrue(decoded.isNotEmpty())
    }

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun `DeviceRegistrationRequest with invalid Base64 public key throws exception`() {
        val invalidPublicKey = "not-valid-base64!!!"

        // Should throw when trying to decode
        assertFailsWith<IllegalArgumentException> {
            Base64.decode(invalidPublicKey)
        }
    }

    @Test
    fun `DeviceRegistrationRequest with empty userProfileId`() {
        val request =
            DeviceRegistrationRequest(
                userProfileId = "",
                deviceToken = "token",
                publicKey = "key",
                platform = Platform.IOS,
            )

        assertEquals("", request.userProfileId)
    }

    @Test
    fun `DeviceRegistrationRequest with empty deviceToken`() {
        val request =
            DeviceRegistrationRequest(
                userProfileId = "user",
                deviceToken = "",
                publicKey = "key",
                platform = Platform.IOS,
            )

        assertEquals("", request.deviceToken)
    }

    @Test
    fun `UnregisterDeviceRequest has correct fields`() {
        val request =
            UnregisterDeviceRequest(
                userProfileId = "user-789",
                deviceToken = "token-012",
            )

        assertEquals("user-789", request.userProfileId)
        assertEquals("token-012", request.deviceToken)
    }

    // Platform Detection Tests

    @Test
    fun `Platform IOS has correct string representation`() {
        assertEquals("IOS", Platform.IOS.toString())
    }

    @Test
    fun `Platform ANDROID has correct string representation`() {
        assertEquals("ANDROID", Platform.ANDROID.toString())
    }

    @Test
    fun `Platform enum can be compared`() {
        assertTrue(Platform.IOS != Platform.ANDROID)
        assertTrue(Platform.IOS == Platform.IOS)
        assertTrue(Platform.ANDROID == Platform.ANDROID)
    }
}
