package network.bisq.mobile.presentation.ui.components.atoms.icons

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.service.IosImageUtil.toByteArray
import org.jetbrains.skia.Image
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation

actual fun rememberPlatformImagePainter(platformImage: PlatformImage): Painter {
    val uiImage = platformImage.image
    val skiaImage = uiImage.toSkiaImage()
    return BitmapPainter(skiaImage.toComposeImageBitmap())
}

fun UIImage.toSkiaImage(): Image {
    val nsData = UIImagePNGRepresentation(this)!!
    val byteArray = nsData.toByteArray()
    return Image.makeFromEncoded(byteArray)
}