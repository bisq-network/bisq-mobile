package network.bisq.mobile.presentation.common.notification

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.data.service.AppForegroundController
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for [ForegroundServiceControllerImpl.refreshNotification] (issue #1749): the refresh
 * must reach the running service as an [ForegroundService.ACTION_REFRESH_NOTIFICATION] intent,
 * and must be a no-op while the service isn't running — a refresh sent to a stopped service
 * would throw from `startService` on modern Android.
 */
@RunWith(RobolectricTestRunner::class)
class ForegroundServiceControllerImplTest {
    private val context: Application = ApplicationProvider.getApplicationContext()

    private lateinit var controller: ForegroundServiceControllerImpl

    @Before
    fun setUp() {
        val appForegroundController: AppForegroundController = mockk(relaxed = true)
        every { appForegroundController.context } returns context
        every { appForegroundController.isForeground } returns MutableStateFlow(false)
        controller = ForegroundServiceControllerImpl(appForegroundController)
    }

    @Test
    fun `refreshNotification is a no-op when the service is not running`() {
        controller.refreshNotification()

        assertNull(Shadows.shadowOf(context).nextStartedService)
    }

    @Test
    fun `refreshNotification sends the refresh action to the running service`() {
        controller.startService()
        val startIntent = Shadows.shadowOf(context).nextStartedService
        assertEquals(ForegroundService::class.java.name, startIntent?.component?.className)

        controller.refreshNotification()

        val refreshIntent = Shadows.shadowOf(context).nextStartedService
        assertEquals(ForegroundService.ACTION_REFRESH_NOTIFICATION, refreshIntent?.action)
        assertEquals(ForegroundService::class.java.name, refreshIntent?.component?.className)
    }
}
