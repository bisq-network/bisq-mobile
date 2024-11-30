package network.bisq.mobile.presentation

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import network.bisq.mobile.PlatformImage

actual fun getPlatformPainter(platformImage: PlatformImage): Painter {
    platformImage as ImageBitmap
    return BitmapPainter(platformImage)
}