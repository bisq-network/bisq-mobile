package network.bisq.mobile.presentation.common.ui.components.atoms.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.presentation.common.ui.utils.IosImageUtil.toByteArray
import org.jetbrains.skia.Image
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation

actual fun getPlatformImagePainter(platformImage: PlatformImage): Painter {
    val uiImage = platformImage.image
    val skiaImage = uiImage.toSkiaImageOrNull()
    return if (skiaImage != null) {
        BitmapPainter(skiaImage.toComposeImageBitmap())
    } else {
        ColorPainter(Color.Transparent)
    }
}

fun UIImage.toSkiaImageOrNull(): Image? {
    val nsData = UIImagePNGRepresentation(this) ?: return null
    val byteArray = nsData.toByteArray()
    return try {
        Image.makeFromEncoded(byteArray)
    } catch (e: Exception) {
        null
    }
}
