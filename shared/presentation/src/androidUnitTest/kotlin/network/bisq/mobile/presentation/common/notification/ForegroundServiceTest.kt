package network.bisq.mobile.presentation.common.notification

import android.app.Service
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import network.bisq.mobile.i18n.I18nSupport
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for [ForegroundService.onStartCommand]'s refresh branch (issue #1749): after a
 * POST_NOTIFICATIONS grant, an [ForegroundService.ACTION_REFRESH_NOTIFICATION] intent must
 * re-run `startForeground` so the notification Android dropped while the permission was
 * denied becomes visible. Without Koin the service falls back to its minimal notification,
 * which is exactly the resilience path the production code documents.
 */
@RunWith(RobolectricTestRunner::class)
class ForegroundServiceTest {
    @Before
    fun setUp() {
        I18nSupport.initialize("en")
    }

    /**
     * Deliberately built WITHOUT `create()`: `onCreate` launches an async notification-upgrade
     * coroutine on Dispatchers.Default with no suspension points, which can outlive the test
     * (even past an @After `destroy()`, since a running coroutine can't be interrupted) and
     * post its notification into a LATER test class's Robolectric environment — seen as
     * NotificationControllerImplTest flaking on `allNotifications.single()`. The refresh
     * branch under test lives entirely in `onStartCommand`, which only needs the attached
     * base context that `buildService` already provides.
     */
    private fun createService(): ForegroundService = Robolectric.buildService(ForegroundService::class.java).get()

    @Test
    fun `onStartCommand with refresh action re-posts the foreground notification and stays sticky`() {
        val service = createService()
        val intent =
            Intent(ApplicationProvider.getApplicationContext(), ForegroundService::class.java)
                .setAction(ForegroundService.ACTION_REFRESH_NOTIFICATION)

        val result = service.onStartCommand(intent, 0, 1)

        assertEquals(Service.START_STICKY, result)
        val shadow = Shadows.shadowOf(service)
        assertNotNull(shadow.lastForegroundNotification)
        assertEquals(ForegroundService.SERVICE_NOTIF_ID, shadow.lastForegroundNotificationId)
    }

    @Test
    fun `onStartCommand without refresh action starts sticky`() {
        val service = createService()

        val result = service.onStartCommand(null, 0, 1)

        assertEquals(Service.START_STICKY, result)
    }
}
