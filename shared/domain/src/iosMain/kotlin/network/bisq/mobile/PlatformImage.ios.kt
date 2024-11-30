@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
@file:OptIn(BetaInteropApi::class)

package network.bisq.mobile

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Foundation.getBytes
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation

actual typealias PlatformImage = UIImage

@OptIn(ExperimentalForeignApi::class)
actual fun ByteArray.toImageBitmap(): PlatformImage {
    // Pin the ByteArray to get a stable memory address
    val nsData = this.usePinned { pinnedByteArray ->
        NSData.create(
            bytes = pinnedByteArray.addressOf(0),
            length = this.size.toULong()
        )
    }
    val uiImage = UIImage(data = nsData) ?: throw IllegalArgumentException("Failed to decode image")
    return uiImage
}

actual fun PlatformImage.toByteArray(): ByteArray {
    val nsData: NSData? = UIImagePNGRepresentation(this)
    return nsData?.toByteArray() ?: ByteArray(0)
}

@OptIn(ExperimentalForeignApi::class)
fun NSData.toByteArray(): ByteArray {
    val length = this.length.toInt()
    val byteArray = ByteArray(length)
    byteArray.usePinned { pinned ->
        this.getBytes(pinned.addressOf(0), length.toULong())
    }
    return byteArray
}
