package network.bisq.mobile.client.common.domain.service.push_notification

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins the fdroid half of the flavor seam.
 *
 * Failing rather than faking success is the load-bearing property: a provider that returned
 * `Result.success` would get the device registered with the trusted node under a token nothing
 * can deliver to, and the user would be told push is on while no notification ever arrives.
 * Documenting that is not enough, so it is asserted here.
 */
class UnsupportedPushNotificationTokenProviderTest {
    @Test
    fun `the flavor seam supplies the unsupported provider`() {
        assertTrue(createPushNotificationTokenProvider() is UnsupportedPushNotificationTokenProvider)
    }

    @Test
    fun `reports that this build has no relayed push transport`() {
        assertFalse(createPushNotificationTokenProvider().isSupported)
    }

    @Test
    fun `never reports permission as granted`() =
        runTest {
            assertFalse(createPushNotificationTokenProvider().requestPermission())
        }

    @Test
    fun `fails the token request instead of faking success`() =
        runTest {
            val result = createPushNotificationTokenProvider().requestDeviceToken()

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is UnsupportedOperationException)
        }

    @Test
    fun `revoking succeeds so opting out still completes`() =
        runTest {
            assertTrue(createPushNotificationTokenProvider().revokeDeviceToken().isSuccess)
        }
}
