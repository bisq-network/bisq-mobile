package network.bisq.mobile.client.common.domain.service.push_notification

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
                deviceToken = "test-token-12345",
                platform = Platform.IOS,
            )

        assertEquals("test-token-12345", request.deviceToken)
        assertEquals(Platform.IOS, request.platform)
    }

    @Test
    fun `DeviceRegistrationRequest for Android platform`() {
        val request =
            DeviceRegistrationRequest(
                deviceToken = "android-fcm-token",
                platform = Platform.ANDROID,
            )

        assertEquals("android-fcm-token", request.deviceToken)
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
}
