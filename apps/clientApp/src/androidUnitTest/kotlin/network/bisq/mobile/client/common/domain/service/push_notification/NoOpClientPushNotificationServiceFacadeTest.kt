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
        assertFalse(facade.isPushNotificationsEnabled.value)
    }

    @Test
    fun `isDeviceRegistered is initially false`() {
        assertFalse(facade.isDeviceRegistered.value)
    }

    @Test
    fun `deviceToken is initially null`() {
        assertNull(facade.deviceToken.value)
    }

    @Test
    fun `requestPermission returns false`() =
        runTest {
            val result = facade.requestPermission()
            assertFalse(result)
        }

    @Test
    fun `registerForPushNotifications returns success`() =
        runTest {
            val result = facade.registerForPushNotifications()
            assertTrue(result.isSuccess)
        }

    @Test
    fun `unregisterFromPushNotifications returns success`() =
        runTest {
            val result = facade.unregisterFromPushNotifications()
            assertTrue(result.isSuccess)
        }

    @Test
    fun `onDeviceTokenReceived does not crash`() =
        runTest {
            // Should not throw
            facade.onDeviceTokenReceived("test-token")
        }

    @Test
    fun `onDeviceTokenRegistrationFailed does not crash`() =
        runTest {
            // Should not throw
            facade.onDeviceTokenRegistrationFailed(RuntimeException("Test error"))
        }

    @Test
    fun `activate does not crash`() =
        runTest {
            // Should not throw
            facade.activate()
        }
}
