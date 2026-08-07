package network.bisq.mobile.presentation.main

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import network.bisq.mobile.data.utils.getPlatformInfo
import network.bisq.mobile.domain.model.PlatformInfo
import network.bisq.mobile.domain.model.PlatformType
import network.bisq.mobile.presentation.common.service.OpenTradesNotificationService
import network.bisq.mobile.presentation.common.test_utils.MainPresenterTestFactory
import network.bisq.mobile.test.presentation.coroutines.PlatformPresentationKoinTestBase
import kotlin.test.Test

/**
 * Tests that MainPresenter cleanup paths execute without blocking the main thread.
 * Verifies the fix for iOS CA Fence hang (issue #1225).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainPresenterCleanupTest : PlatformPresentationKoinTestBase() {
    override val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    @Test
    fun `onDestroy calls cleanupNotificationService without blocking`() =
        runBlocking {
            val notificationService = mockk<OpenTradesNotificationService>(relaxed = true)
            val presenter =
                MainPresenterTestFactory.create(
                    openTradesNotificationService = notificationService,
                )
            presenter.onDestroy()
            // Allow fire-and-forget coroutine to complete
            delay(100)
            coVerify { notificationService.stopNotificationService() }
        }

    @Test
    fun `onDestroy resets GlobalUiManager without disposing its scope`() =
        runBlocking {
            // Regression guard for issue #1562: GlobalUiManager is an app-lifetime singleton, so
            // teardown must reset() its transient loading state, never dispose() (cancel its scope).
            val presenter = MainPresenterTestFactory.create()
            presenter.onDestroy()
            delay(100)
            verify(exactly = 1) { globalUiManager.reset() }
            verify(exactly = 0) { globalUiManager.dispose() }
        }

    @Test
    fun `onDestroy completes without error when notification service throws`() =
        runBlocking {
            val notificationService = mockk<OpenTradesNotificationService>()
            coEvery { notificationService.stopNotificationService() } throws RuntimeException("Service error")
            val presenter =
                MainPresenterTestFactory.create(
                    openTradesNotificationService = notificationService,
                )
            // Should not throw even if notification service fails
            presenter.onDestroy()
            delay(100)
        }

    @Test
    fun `cleanupNotificationService uses fire-and-forget on iOS`() =
        runBlocking {
            mockkStatic("network.bisq.mobile.data.utils.PlatformDomainAbstractions_androidKt")
            try {
                val iosPlatformInfo =
                    object : PlatformInfo {
                        override val name = "iOS"
                        override val type = PlatformType.IOS
                    }
                every { getPlatformInfo() } returns iosPlatformInfo

                val notificationService = mockk<OpenTradesNotificationService>(relaxed = true)
                val presenter =
                    MainPresenterTestFactory.create(
                        openTradesNotificationService = notificationService,
                    )
                presenter.cleanupNotificationService()
                // Allow fire-and-forget coroutine to complete (Dispatchers.Default)
                delay(200)
                coVerify { notificationService.stopNotificationService() }
            } finally {
                unmockkStatic("network.bisq.mobile.data.utils.PlatformDomainAbstractions_androidKt")
            }
        }

    @Test
    fun `cleanupNotificationService logs error on iOS when service fails`() =
        runBlocking {
            mockkStatic("network.bisq.mobile.data.utils.PlatformDomainAbstractions_androidKt")
            try {
                val iosPlatformInfo =
                    object : PlatformInfo {
                        override val name = "iOS"
                        override val type = PlatformType.IOS
                    }
                every { getPlatformInfo() } returns iosPlatformInfo

                val notificationService = mockk<OpenTradesNotificationService>()
                coEvery { notificationService.stopNotificationService() } throws RuntimeException("iOS service error")
                val presenter =
                    MainPresenterTestFactory.create(
                        openTradesNotificationService = notificationService,
                    )
                // Should not throw — error is caught by runCatching and logged
                presenter.cleanupNotificationService()
                delay(200)
            } finally {
                unmockkStatic("network.bisq.mobile.data.utils.PlatformDomainAbstractions_androidKt")
            }
        }
}
