package network.bisq.mobile.presentation

import androidx.compose.ui.graphics.painter.Painter
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.presentation.ui.helpers.TimeProvider

expect fun getPlatformPainter(platformImage: PlatformImage): Painter

expect fun getPlatformCurrentTimeProvider(): TimeProvider

expect fun moveAppToBackground(view: Any?)

expect object ScreenInfo {
    val density: Float        // The scaling factor compared to the default dpi
    val densityDpi: Int       // e.g. 160, 320, 480
    val widthPixels: Int
}
