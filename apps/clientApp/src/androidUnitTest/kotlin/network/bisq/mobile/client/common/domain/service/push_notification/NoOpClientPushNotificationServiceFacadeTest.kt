package network.bisq.mobile.client.common.domain.service.push_notification

import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NoOpClientPushNotificationServiceFacadeTest {
    private lateinit var facade: NoOpClientPushNotificationServiceFacade

    @Before
    fun setup() {
        facade = NoOpClientPushNotificationServiceFacade()
    }

    @Test
    fun `isPushNotificationsEnabled is initially false`() {
        // Given
        // NoOp facade is initialized

        // When
        val result = facade.isPushNotificationsEnabled.value

        // Then
        assertFalse(result)
    }

    @Test
    fun `isDeviceRegistered is initially false`() {
        // Given
        // NoOp facade is initialized

        // When
        val result = facade.isDeviceRegistered.value

        // Then
        assertFalse(result)
    }

    @Test
    fun `deviceToken is initially null`() {
        // Given
        // NoOp facade is initialized

        // When
        val result = facade.deviceToken.value

        // Then
        assertNull(result)
    }

    @Test
    fun `requestPermission returns false`() =
        runTest {
            // Given
            // NoOp facade is initialized

            // When
            val result = facade.requestPermission()

            // Then
            assertFalse(result)
        }

    @Test
    fun `registerForPushNotifications returns success`() =
        runTest {
            // Given
            // NoOp facade is initialized

            // When
            val result = facade.registerForPushNotifications()

            // Then
            assertTrue(result.isSuccess)
        }

    @Test
    fun `unregisterFromPushNotifications returns success`() =
        runTest {
            // Given
            // NoOp facade is initialized

            // When
            val result = facade.unregisterFromPushNotifications()

            // Then
            assertTrue(result.isSuccess)
        }

    @Test
    fun `onDeviceTokenReceived does not crash`() =
        runTest {
            // Given
            val token = "test-token"

            // When / Then - should not throw
            facade.onDeviceTokenReceived(token)
        }

    @Test
    fun `onDeviceTokenRegistrationFailed does not crash`() =
        runTest {
            // Given
            val error = RuntimeException("Test error")

            // When / Then - should not throw
            facade.onDeviceTokenRegistrationFailed(error)
        }

    @Test
    fun `activate does not crash`() =
        runTest {
            // Given
            // NoOp facade is initialized

            // When / Then - should not throw
            facade.activate()
        }
}
