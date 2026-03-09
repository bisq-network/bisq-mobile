package network.bisq.mobile.presentation.main

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.domain.PlatformInfo
import network.bisq.mobile.domain.PlatformType
import network.bisq.mobile.domain.getPlatformInfo
import network.bisq.mobile.domain.utils.CoroutineExceptionHandlerSetup
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.DefaultCoroutineJobsManager
import network.bisq.mobile.presentation.common.service.OpenTradesNotificationService
import network.bisq.mobile.presentation.common.test_utils.MainPresenterTestFactory
import network.bisq.mobile.presentation.common.test_utils.NoopNavigationManager
import network.bisq.mobile.presentation.common.test_utils.TestApplicationLifecycleService
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.platform.getScreenWidthDp
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Tests that MainPresenter cleanup paths execute without blocking the main thread.
 * Verifies the fix for iOS CA Fence hang (issue #1225).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainPresenterCleanupTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        every { getScreenWidthDp() } returns 480
        startKoin {
            modules(
                module {
                    single { CoroutineExceptionHandlerSetup() }
                    factory<CoroutineJobsManager> {
                        DefaultCoroutineJobsManager().apply {
                            get<CoroutineExceptionHandlerSetup>().setupExceptionHandler(this)
                        }
                    }
                    single { NoopNavigationManager() as network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager }
                    single { GlobalUiManager() }
                },
            )
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
        unmockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
    }

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
            // Mock getPlatformInfo to return iOS
            mockkStatic("network.bisq.mobile.domain.PlatformDomainAbstractions_androidKt")
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
            // Allow fire-and-forget coroutine to complete
            delay(200)
            coVerify { notificationService.stopNotificationService() }

            unmockkStatic("network.bisq.mobile.domain.PlatformDomainAbstractions_androidKt")
        }

    @Test
    fun `cleanupNotificationService logs error on iOS when service fails`() =
        runBlocking {
            mockkStatic("network.bisq.mobile.domain.PlatformDomainAbstractions_androidKt")
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

            unmockkStatic("network.bisq.mobile.domain.PlatformDomainAbstractions_androidKt")
        }
}
