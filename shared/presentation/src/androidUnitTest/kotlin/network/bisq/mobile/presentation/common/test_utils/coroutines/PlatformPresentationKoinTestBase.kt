package network.bisq.mobile.presentation.common.test_utils.coroutines

import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import network.bisq.mobile.presentation.common.ui.platform.getScreenWidthDp

abstract class PlatformPresentationKoinTestBase : PresentationKoinTestBase() {
    override fun setUpPlatformMocks() {
        mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        every { getScreenWidthDp() } returns 480
    }

    override fun tearDownPlatformMocks() {
        unmockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
    }
}
