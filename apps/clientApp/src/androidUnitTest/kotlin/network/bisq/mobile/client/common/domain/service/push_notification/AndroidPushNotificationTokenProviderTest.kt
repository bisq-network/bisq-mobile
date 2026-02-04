package network.bisq.mobile.client.common.domain.service.push_notification

import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidPushNotificationTokenProviderTest {
    private lateinit var provider: AndroidPushNotificationTokenProvider

    @Before
    fun setup() {
        provider = AndroidPushNotificationTokenProvider()
    }

    @Test
    fun `requestPermission returns false when FCM not implemented`() =
        runTest {
            // Given
            // Provider is initialized without FCM implementation

            // When
            val result = provider.requestPermission()

            // Then
            assertFalse(result)
        }

    @Test
    fun `requestDeviceToken returns failure when FCM not implemented`() =
        runTest {
            // Given
            // Provider is initialized without FCM implementation

            // When
            val result = provider.requestDeviceToken()

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is PushNotificationException)
            assertTrue(result.exceptionOrNull()?.message?.contains("not yet implemented") == true)
        }
}
