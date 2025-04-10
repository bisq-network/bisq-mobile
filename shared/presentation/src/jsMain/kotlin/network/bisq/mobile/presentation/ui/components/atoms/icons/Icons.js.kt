package network.bisq.mobile.presentation.ui.components.atoms.icons

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import network.bisq.mobile.domain.PlatformImage
import org.jetbrains.skia.Image

actual fun rememberPlatformImagePainter(platformImage: PlatformImage): Painter {
    // For web, convert the data URL to a Skia image and then to a Compose ImageBitmap
    val dataUrl = platformImage.dataUrl
    val skiaImage = Image.makeFromEncoded(dataUrl.encodeToByteArray())
    return BitmapPainter(skiaImage.toComposeImageBitmap())
}