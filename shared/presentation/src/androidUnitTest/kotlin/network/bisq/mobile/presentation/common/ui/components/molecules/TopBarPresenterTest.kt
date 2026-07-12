package network.bisq.mobile.presentation.common.ui.components.molecules

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.utils.DeviceInfoProvider
import network.bisq.mobile.presentation.common.test_utils.MainPresenterTestFactory
import network.bisq.mobile.presentation.common.test_utils.coroutines.PlatformPresentationKoinTestBase
import network.bisq.mobile.presentation.common.ui.animation.AnimationSettings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TopBarPresenterTest : PlatformPresentationKoinTestBase() {
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
