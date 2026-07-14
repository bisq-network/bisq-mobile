package network.bisq.mobile.presentation.common.test_utils.coroutines

import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import network.bisq.mobile.presentation.common.ui.platform.getScreenWidthDp

private const val PLATFORM_PRESENTATION_ABSTRATIONS =
    "network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt"

object PlatformStaticMocks {
    fun mockScreenWidth(widthDp: Int) {
        mockkStatic(PLATFORM_PRESENTATION_ABSTRATIONS)
        every { getScreenWidthDp() } returns widthDp
    }

    fun unmockScreenWidth() {
        unmockkStatic(PLATFORM_PRESENTATION_ABSTRATIONS)
    }
}
