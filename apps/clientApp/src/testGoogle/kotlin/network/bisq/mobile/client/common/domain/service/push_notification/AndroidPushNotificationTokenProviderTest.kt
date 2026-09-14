package network.bisq.mobile.client.common.domain.service.push_notification

import android.Manifest
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.client.common.test_utils.TestApplication
import network.bisq.mobile.data.utils.AndroidAppContext
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.IOException
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [28]) // Default SDK; per-test overrides apply where noted.
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
        unmockkAll() // tear down any mockkStatic done in individual tests
    }

    // ----- requestPermission -----

    @Test
    fun `requestPermission returns true on pre-Android-13 (POST_NOTIFICATIONS not required)`() =
        runTest {
            // SDK 28 is below TIRAMISU (33) — runtime permission is auto-granted.
            assertTrue(provider.requestPermission())
        }

    @Test
    @Config(sdk = [33])
    fun `requestPermission returns true on Android 13+ when POST_NOTIFICATIONS is granted`() =
        runTest {
            val shadowApp = Shadows.shadowOf(ApplicationProvider.getApplicationContext<Application>())
            shadowApp.grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

            assertTrue(provider.requestPermission())
        }

    @Test
    @Config(sdk = [33])
    fun `requestPermission returns false on Android 13+ when POST_NOTIFICATIONS is denied`() =
        runTest {
            val shadowApp = Shadows.shadowOf(ApplicationProvider.getApplicationContext<Application>())
            shadowApp.denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

            assertFalse(provider.requestPermission())
        }

    // ----- requestDeviceToken -----

    @Test
    fun `requestDeviceToken fails when Firebase project is not configured (placeholder google-services_json)`() =
        runTest {
            // No mockk — exercises the real failure path. The placeholder
            // google-services.json has bogus IDs, so token retrieval surfaces
            // as a PushNotificationException with a configuration hint.
            val result = provider.requestDeviceToken()

            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertTrue(exception is PushNotificationException)
            assertTrue(
                exception.message?.contains("google-services") == true,
                "expected hint about google-services.json, got: ${exception.message}",
            )
        }

    @Test
    fun `requestDeviceToken rolls back auto-init when token fetch throws`() =
        runTest {
            val mockMessaging = mockk<FirebaseMessaging>(relaxed = true)
            mockkStatic(FirebaseMessaging::class)
            every { FirebaseMessaging.getInstance() } returns mockMessaging
            every { mockMessaging.token } returns Tasks.forException(RuntimeException("simulated network error"))

            val result = provider.requestDeviceToken()

            assertTrue(result.isFailure)
            // Auto-init was flipped to true at entry (user opted in), then
            // rolled back to false in the recoverCatching branch — the device
            // must not be left in a half-on state when registration fails.
            verifyOrder {
                mockMessaging.isAutoInitEnabled = true
                mockMessaging.isAutoInitEnabled = false
            }
        }

    // ----- revokeDeviceToken -----

    @Test
    fun `revokeDeviceToken succeeds when deleteToken succeeds and disables auto-init`() =
        runTest {
            val mockMessaging = mockk<FirebaseMessaging>(relaxed = true)
            mockkStatic(FirebaseMessaging::class)
            every { FirebaseMessaging.getInstance() } returns mockMessaging
            every { mockMessaging.deleteToken() } returns Tasks.forResult(null as Void?)

            val result = provider.revokeDeviceToken()

            assertTrue(result.isSuccess)
            verify { mockMessaging.deleteToken() }
            verify { mockMessaging.isAutoInitEnabled = false }
        }

    @Test
    fun `revokeDeviceToken propagates deleteToken failure but still disables auto-init`() =
        runTest {
            // The whole point of the try / finally refactor: even when
            // deleteToken throws, we still want to cut the connection to
            // Google's servers locally so the device stops behaving like an
            // opted-in client.
            val mockMessaging = mockk<FirebaseMessaging>(relaxed = true)
            mockkStatic(FirebaseMessaging::class)
            every { FirebaseMessaging.getInstance() } returns mockMessaging
            every { mockMessaging.deleteToken() } returns Tasks.forException(IOException("FCM unreachable"))

            val result = provider.revokeDeviceToken()

            assertTrue(result.isFailure, "deleteToken failure must propagate as Result.failure")
            verify { mockMessaging.isAutoInitEnabled = false } // ran via finally
        }
}
