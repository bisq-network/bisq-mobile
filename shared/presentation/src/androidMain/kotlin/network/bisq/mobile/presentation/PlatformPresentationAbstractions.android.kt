package network.bisq.mobile.presentation

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import network.bisq.mobile.domain.PlatformImage

actual fun getPlatformPainter(platformImage: PlatformImage): Painter {
    return BitmapPainter(platformImage.bitmap)
}