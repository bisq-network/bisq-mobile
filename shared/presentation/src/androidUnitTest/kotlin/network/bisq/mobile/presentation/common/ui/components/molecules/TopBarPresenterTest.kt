package network.bisq.mobile.presentation.common.ui.components.molecules

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.DeviceInfoProvider
import network.bisq.mobile.presentation.common.test_utils.MainPresenterTestFactory
import network.bisq.mobile.presentation.common.test_utils.NoopNavigationManager
import network.bisq.mobile.presentation.common.ui.animation.AnimationSettings
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.platform.getScreenWidthDp
import network.bisq.mobile.test.coroutines.TestCoroutineJobsManager
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TopBarPresenterTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        every { getScreenWidthDp() } returns 480
        startKoin {
            modules(
                module {
                    factory<CoroutineJobsManager> { TestCoroutineJobsManager(testDispatcher) }
                    single<NavigationManager> { NoopNavigationManager() }
                    single { GlobalUiManager(testDispatcher) }
                },
            )
        }
    }

    @AfterTest
    fun tearDown() {
        unmockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        Dispatchers.resetMain()
        stopKoin()
    }

    private fun device(ramBytes: Long): DeviceInfoProvider =
        object : DeviceInfoProvider {
            override fun getDeviceInfo(): String = ""

            override fun getTotalRamBytes(): Long = ramBytes
        }

    private fun animationSettings(
        userSetting: Boolean,
        applyDeviceLock: Boolean,
        ramBytes: Long,
    ): AnimationSettings {
        val settings = mockk<SettingsServiceFacade>(relaxed = true)
        every { settings.useAnimations } returns MutableStateFlow(userSetting)
        return AnimationSettings(settings, device(ramBytes), applyDeviceLock)
    }

    private fun presenter(animationSettings: AnimationSettings): TopBarPresenter =
        TopBarPresenter(
            userProfileServiceFacade = mockk(relaxed = true),
            settingsServiceFacade = mockk(relaxed = true),
            connectivityService = mockk(relaxed = true),
            animationSettings = animationSettings,
            mainPresenter = MainPresenterTestFactory.create(),
        )

    // 3GB device reports ~2.8 GiB (low-spec).
    private val lowSpecRam = 2_900_000_000L

    @Test
    fun `showAnimation follows the user setting when device is not locked`() {
        assertTrue(presenter(animationSettings(userSetting = true, applyDeviceLock = false, ramBytes = lowSpecRam)).showAnimation.value)
    }

    @Test
    fun `showAnimation is forced off on a low-spec locked device`() {
        // User setting is ON but the node-side device lock forces the avatar animation off.
        assertFalse(presenter(animationSettings(userSetting = true, applyDeviceLock = true, ramBytes = lowSpecRam)).showAnimation.value)
    }
}
