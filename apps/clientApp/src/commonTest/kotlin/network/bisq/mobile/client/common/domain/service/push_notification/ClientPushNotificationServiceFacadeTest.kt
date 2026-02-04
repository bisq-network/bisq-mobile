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
    fun `when accessing Platform enum then has correct values`() {
        // Given - Platform enum

        // When/Then
        assertEquals("IOS", Platform.IOS.name)
        assertEquals("ANDROID", Platform.ANDROID.name)
        assertEquals(2, Platform.entries.size)
    }

    @Test
    fun `when creating DeviceRegistrationRequest for iOS then contains correct data`() {
        // Given
        val deviceId = "device-id-123"
        val deviceToken = "test-token-12345"
        val publicKeyBase64 = "base64-encoded-public-key"
        val deviceDescriptor = "iPhone 15 Pro, iOS 17.2"

        // When
        val request =
            DeviceRegistrationRequest(
                deviceId = deviceId,
                deviceToken = deviceToken,
                publicKeyBase64 = publicKeyBase64,
                deviceDescriptor = deviceDescriptor,
                platform = Platform.IOS,
            )

        // Then
        assertEquals(deviceId, request.deviceId)
        assertEquals(deviceToken, request.deviceToken)
        assertEquals(publicKeyBase64, request.publicKeyBase64)
        assertEquals(deviceDescriptor, request.deviceDescriptor)
        assertEquals(Platform.IOS, request.platform)
    }

    @Test
    fun `when creating DeviceRegistrationRequest for Android then contains correct data`() {
        // Given
        val deviceId = "device-id-456"
        val deviceToken = "android-fcm-token"
        val publicKeyBase64 = "base64-encoded-public-key-android"
        val deviceDescriptor = "Pixel 8 Pro, Android 14"

        // When
        val request =
            DeviceRegistrationRequest(
                deviceId = deviceId,
                deviceToken = deviceToken,
                publicKeyBase64 = publicKeyBase64,
                deviceDescriptor = deviceDescriptor,
                platform = Platform.ANDROID,
            )

        // Then
        assertEquals(deviceId, request.deviceId)
        assertEquals(deviceToken, request.deviceToken)
        assertEquals(publicKeyBase64, request.publicKeyBase64)
        assertEquals(deviceDescriptor, request.deviceDescriptor)
        assertEquals(Platform.ANDROID, request.platform)
    }

    @Test
    fun `when creating PushNotificationException with message then contains message`() {
        // Given
        val errorMessage = "Test error message"

        // When
        val exception = PushNotificationException(errorMessage)

        // Then
        assertEquals(errorMessage, exception.message)
    }

    @Test
    fun `when creating PushNotificationException with cause then contains cause`() {
        // Given
        val cause = RuntimeException("Root cause")
        val errorMessage = "Test error"

        // When
        val exception = PushNotificationException(errorMessage, cause)

        // Then
        assertEquals(errorMessage, exception.message)
        assertEquals(cause, exception.cause)
    }

    // ========== Device Registration Validation Tests ==========

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun `when DeviceRegistrationRequest has valid Base64 public key then decodes successfully`() {
        // Given
        val testPublicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE" // Valid Base64

        // When
        val request =
            DeviceRegistrationRequest(
                deviceId = "device-123",
                deviceToken = "device-token-456",
                publicKeyBase64 = testPublicKey,
                deviceDescriptor = "iPhone 15 Pro, iOS 17.2",
                platform = Platform.IOS,
            )
        val decoded = Base64.decode(request.publicKeyBase64)

        // Then
        assertNotNull(decoded)
        assertTrue(decoded.isNotEmpty())
    }

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun `when decoding invalid Base64 public key then throws exception`() {
        // Given
        val invalidPublicKey = "not-valid-base64!!!"

        // When/Then
        assertFailsWith<IllegalArgumentException> {
            Base64.decode(invalidPublicKey)
        }
    }

    @Test
    fun `when DeviceRegistrationRequest has empty deviceId then allows creation`() {
        // Given
        val emptyDeviceId = ""

        // When
        val request =
            DeviceRegistrationRequest(
                deviceId = emptyDeviceId,
                deviceToken = "token",
                publicKeyBase64 = "key",
                deviceDescriptor = "iPhone",
                platform = Platform.IOS,
            )

        // Then
        assertEquals(emptyDeviceId, request.deviceId)
    }

    @Test
    fun `when DeviceRegistrationRequest has empty deviceToken then allows creation`() {
        // Given
        val emptyDeviceToken = ""

        // When
        val request =
            DeviceRegistrationRequest(
                deviceId = "device-id",
                deviceToken = emptyDeviceToken,
                publicKeyBase64 = "key",
                deviceDescriptor = "iPhone",
                platform = Platform.IOS,
            )

        // Then
        assertEquals(emptyDeviceToken, request.deviceToken)
    }

    // ========== Platform Detection Tests ==========

    @Test
    fun `when accessing Platform IOS then has correct string representation`() {
        // Given - Platform.IOS

        // When
        val result = Platform.IOS.toString()

        // Then
        assertEquals("IOS", result)
    }

    @Test
    fun `when accessing Platform ANDROID then has correct string representation`() {
        // Given - Platform.ANDROID

        // When
        val result = Platform.ANDROID.toString()

        // Then
        assertEquals("ANDROID", result)
    }

    @Test
    fun `when comparing Platform enum values then correctly distinguishes them`() {
        // Given - Platform enum values

        // When/Then
        assertTrue(Platform.IOS != Platform.ANDROID)
        assertTrue(Platform.IOS == Platform.IOS)
        assertTrue(Platform.ANDROID == Platform.ANDROID)
    }

    // ========== PlatformMapper Tests ==========

    @Test
    fun `when mapping IOS PlatformType then returns IOS Platform`() {
        // Given
        val platformType = network.bisq.mobile.domain.PlatformType.IOS

        // When
        val platform = PlatformMapper.fromPlatformType(platformType)

        // Then
        assertEquals(Platform.IOS, platform)
    }

    @Test
    fun `when mapping ANDROID PlatformType then returns ANDROID Platform`() {
        // Given
        val platformType = network.bisq.mobile.domain.PlatformType.ANDROID

        // When
        val platform = PlatformMapper.fromPlatformType(platformType)

        // Then
        assertEquals(Platform.ANDROID, platform)
    }
}
