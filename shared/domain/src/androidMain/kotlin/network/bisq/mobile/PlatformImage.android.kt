@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package network.bisq.mobile

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import network.bisq.mobile.utils.AndroidImageUtil

// FIXME if ImageBitmap is used it gives a compile error
actual typealias PlatformImage = Any

actual fun ByteArray.toImageBitmap(): PlatformImage {
    val bitmap:Bitmap = AndroidImageUtil.decodePngToImageBitmap(this)
    val asImageBitmap: ImageBitmap = bitmap.asImageBitmap()
    return asImageBitmap
}

actual fun PlatformImage.toByteArray(): ByteArray {
    this as ImageBitmap
    return AndroidImageUtil.bitmapToByteArray(this.asAndroidBitmap())
}
