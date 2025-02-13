package network.bisq.mobile.presentation

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.presentation.ui.helpers.AndroidCurrentTimeProvider
import network.bisq.mobile.presentation.ui.helpers.TimeProvider

actual fun getPlatformPainter(platformImage: PlatformImage): Painter {
    return BitmapPainter(platformImage.bitmap)
}

actual fun getPlatformCurrentTimeProvider(): TimeProvider = AndroidCurrentTimeProvider()

actual fun exitApp() {
    // not used in Android
}
