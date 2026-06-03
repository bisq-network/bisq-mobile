package network.bisq.mobile.presentation.common.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.data.service.ForegroundDetector
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.common.notification.ForegroundServiceController
import network.bisq.mobile.presentation.common.notification.NotificationController
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Direct unit tests for the foreground-service-vs-local-delivery state machine on
 * [OpenTradesNotificationService] — the seam where bisq-mobile#1450's regression lived.
 *
 * Why this lives separately from [OpenTradesNotificationServiceStateTest]: that suite
 * focuses on the trade-state notify(…) decisions; this one focuses on the lifecycle
 * decoupling between [OpenTradesNotificationService.setKeepProcessAlive] (controls the
 * Android FG service) and [OpenTradesNotificationService.setLocalDeliverySuppressed]
 * (controls whether observers post notifications). The previous regression was caused
 * by these two concerns being tangled in a single method — without a direct test for
 * the decoupled contract, only an indirect test via `ClientApplicationLifecycleService`
 * caught the bug. This suite pins the decoupled contract so a future re-tangle fails
 * a unit test rather than a manual test on device.
 */
class OpenTradesNotificationServiceLifecycleTest {
    private lateinit var notificationController: NotificationController
    private lateinit var foregroundServiceController: ForegroundServiceController
    private lateinit var tradesServiceFacade: TradesServiceFacade
    private lateinit var userProfileServiceFacade: UserProfileServiceFacade
    private lateinit var appForegroundController: ForegroundDetector
    private lateinit var service: OpenTradesNotificationService

    @BeforeTest
    fun setup() {
        I18nSupport.initialize("en")

        notificationController = mockk(relaxed = true)
        foregroundServiceController = mockk(relaxed = true)

        tradesServiceFacade = mockk(relaxed = true)
        every { tradesServiceFacade.openTradeItems } returns MutableStateFlow(emptyList())

        userProfileServiceFacade = mockk(relaxed = true)
        every { userProfileServiceFacade.ignoredProfileIds } returns MutableStateFlow(emptySet())

        appForegroundController = mockk(relaxed = true)
        // Foreground=true at setup means the lifecycle observer won't try to register
        // background flow observers — keeps the tests focused on the FG service /
        // suppression seam rather than the BG observer-registration path.
        every { appForegroundController.isForeground } returns MutableStateFlow(true)

        service =
            OpenTradesNotificationService(
                notificationController = notificationController,
                foregroundServiceController = foregroundServiceController,
                tradesServiceFacade = tradesServiceFacade,
                userProfileServiceFacade = userProfileServiceFacade,
                appForegroundController = appForegroundController,
            )
    }

    @Test
    fun `setKeepProcessAlive true starts the foreground service when not running`() =
        runTest {
            service.setKeepProcessAlive(true)

            verify(exactly = 1) { foregroundServiceController.startService() }
            verify(exactly = 0) { foregroundServiceController.stopService() }
        }

    @Test
    fun `setKeepProcessAlive false stops the foreground service when running`() =
        runTest {
            service.setKeepProcessAlive(true)

            service.setKeepProcessAlive(false)

            verify(exactly = 1) { foregroundServiceController.startService() }
            verify(exactly = 1) { foregroundServiceController.stopService() }
        }

    @Test
    fun `setKeepProcessAlive is idempotent — repeated same-value calls are no-ops`() =
        runTest {
            // True → true → true should call startService exactly once.
            service.setKeepProcessAlive(true)
            service.setKeepProcessAlive(true)
            service.setKeepProcessAlive(true)
            verify(exactly = 1) { foregroundServiceController.startService() }
            verify(exactly = 0) { foregroundServiceController.stopService() }

            // False → false → false (after returning to stopped) should call stopService
            // exactly once total, not three times.
            service.setKeepProcessAlive(false)
            service.setKeepProcessAlive(false)
            verify(exactly = 1) { foregroundServiceController.stopService() }
        }

    @Test
    fun `setLocalDeliverySuppressed does NOT touch the foreground service`() =
        runTest {
            // This is the regression-pin for bisq-mobile#1450. Before the fix,
            // `setLocalDeliverySuppressed(true)` ALSO stopped the FG service. That
            // killed the keep-connected-in-background mode for users running
            // relayed+keepConnected=on. The decoupled contract: suppression only
            // gates `notify(…)` / observer registration. FG lifecycle is owned
            // by `setKeepProcessAlive`.
            service.setKeepProcessAlive(true)

            service.setLocalDeliverySuppressed(true)
            service.setLocalDeliverySuppressed(false)
            service.setLocalDeliverySuppressed(true)

            verify(exactly = 1) { foregroundServiceController.startService() }
            verify(exactly = 0) { foregroundServiceController.stopService() }
        }

    @Test
    fun `startService is a backward-compatible alias for setKeepProcessAlive true`() =
        runTest {
            // `startService()` is still called by the nodeApp's lifecycle service
            // (which doesn't differentiate between FG lifecycle and local notification
            // posting — local IS the only path on the node app). Verify it stays
            // wired to FG-start so the nodeApp keeps working without changes.
            service.startService()

            verify(exactly = 1) { foregroundServiceController.startService() }
            verify(exactly = 0) { foregroundServiceController.stopService() }
        }
}
