package network.bisq.mobile.presentation.ui.components.atoms.icons

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import network.bisq.mobile.domain.PlatformImage

actual fun getPlatformImagePainter(platformImage: PlatformImage): Painter {
    return BitmapPainter(platformImage.bitmap)
}