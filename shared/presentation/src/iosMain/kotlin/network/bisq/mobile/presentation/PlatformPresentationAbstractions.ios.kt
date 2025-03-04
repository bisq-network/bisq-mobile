@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package network.bisq.mobile.presentation

import platform.UIKit.UIScreen
import platform.CoreGraphics.CGRectGetWidth
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.presentation.ui.helpers.IOSCurrentTimeProvider
import network.bisq.mobile.presentation.ui.helpers.TimeProvider
import platform.CoreGraphics.CGContextRef
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.NSSelectorFromString
import platform.Foundation.getBytes
import platform.UIKit.*

@OptIn(ExperimentalForeignApi::class)
fun NSData.toByteArray(): ByteArray {
    val length = this.length.toInt()
    val byteArray = ByteArray(length)
    byteArray.usePinned { pinned ->
        this.getBytes(pinned.addressOf(0), length.toULong())
    }
    return byteArray
}

@OptIn(ExperimentalForeignApi::class)
actual fun getPlatformPainter(platformImage: PlatformImage): Painter {
    return object : Painter() {
        override val intrinsicSize: Size
            get() {
                val size: CValue<CGSize> = platformImage.image.size
                return Size(
                    width = size.useContents { this.width.toFloat() },
                    height = size.useContents { this.width.toFloat() },
                )
            }

        override fun DrawScope.onDraw() {
            UIGraphicsBeginImageContextWithOptions(
                size = CGSizeMake(size.width.toDouble(), size.height.toDouble()),
                opaque = false,
                scale = UIScreen.mainScreen.scale
            )
            val context: CGContextRef? = UIGraphicsGetCurrentContext()
            if (context != null) {
                platformImage.image.drawInRect(
                    CGRectMake(0.0, 0.0, size.width.toDouble(), size.height.toDouble())
                )
            }
            UIGraphicsEndImageContext()
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun exitApp() {
    UIApplication.sharedApplication.performSelector(NSSelectorFromString("suspend"))
}

actual fun getPlatformCurrentTimeProvider(): TimeProvider = IOSCurrentTimeProvider()

@OptIn(ExperimentalForeignApi::class)
actual fun getScreenWidthDp(): Int {
    return CGRectGetWidth(UIScreen.mainScreen.bounds).toInt()
}