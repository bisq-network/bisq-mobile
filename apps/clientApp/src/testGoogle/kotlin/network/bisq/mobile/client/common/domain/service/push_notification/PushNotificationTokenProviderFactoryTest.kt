package network.bisq.mobile.client.common.domain.service.push_notification

import androidx.test.core.app.ApplicationProvider
import network.bisq.mobile.client.common.test_utils.TestApplication
import network.bisq.mobile.data.utils.AndroidAppContext
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertTrue

/**
 * The google half of the flavor seam. Its counterpart lives in `src/testFdroid`; the two
 * factories share a signature and nothing but the flavor decides which one compiles, so each
 * side is pinned where it is built.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [28])
class PushNotificationTokenProviderFactoryTest {
    @Before
    fun setup() {
        AndroidAppContext.initialize(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() {
        AndroidAppContext.reset()
    }

    @Test
    fun `the flavor seam supplies the FCM provider`() {
        assertTrue(createPushNotificationTokenProvider() is AndroidPushNotificationTokenProvider)
    }

    @Test
    fun `the FCM provider reports a working transport`() {
        assertTrue(createPushNotificationTokenProvider().isSupported)
    }
}
