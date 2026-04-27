package network.bisq.mobile.client.common.domain.service.push_notification

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.client.common.test_utils.TestApplication
import network.bisq.mobile.data.utils.AndroidAppContext
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [28]) // Pre-Tiramisu — POST_NOTIFICATIONS not required at runtime
class AndroidPushNotificationTokenProviderTest {
    private lateinit var provider: AndroidPushNotificationTokenProvider

    @Before
    fun setup() {
        AndroidAppContext.initialize(ApplicationProvider.getApplicationContext())
        provider = AndroidPushNotificationTokenProvider()
    }

    @After
    fun tearDown() {
        AndroidAppContext.reset()
    }

    @Test
    fun `requestPermission returns true on pre-Android-13 (POST_NOTIFICATIONS not required)`() =
        runTest {
            // SDK 28 is below TIRAMISU (33) — runtime permission is auto-granted.
            assertTrue(provider.requestPermission())
        }

    @Test
    fun `requestDeviceToken fails when Firebase project is not configured (placeholder google-services_json)`() =
        runTest {
            // The placeholder google-services.json has bogus IDs, so token retrieval
            // surfaces as a PushNotificationException with a configuration hint.
            val result = provider.requestDeviceToken()

            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertTrue(exception is PushNotificationException)
            assertTrue(
                exception.message?.contains("google-services") == true,
                "expected hint about google-services.json, got: ${exception.message}",
            )
        }
}
