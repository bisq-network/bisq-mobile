package network.bisq.mobile.presentation.common.test_utils.compose

import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import network.bisq.mobile.presentation.common.ui.platform.getScreenWidthDp

/**
 * Compose + Koin base for presentation UI tests that also need platform static mocks
 * (e.g. screen width). Inherits Compose behavior from [PresentationKoinComposeTestBase].
 */
abstract class PlatformPresentationKoinComposeTestBase : PresentationKoinComposeTestBase() {
    override fun setUpPlatformMocks() {
        mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        every { getScreenWidthDp() } returns 480
    }

    override fun tearDownPlatformMocks() {
        unmockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
    }
}
