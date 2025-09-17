@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package network.bisq.mobile.presentation

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
import platform.CoreGraphics.CGRectGetWidth
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.getBytes
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetCurrentContext
import platform.UIKit.UIScreen

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
                    height = size.useContents { this.height.toFloat() },
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

actual fun moveAppToBackground(view: Any?) {
    // NSSelectorFromString("suspend") is a private API, which violates App Store guidelines
    // UIApplication.sharedApplication.performSelector(NSSelectorFromString("suspend"))
    // iOS apps are not allowed to programmatically go Home.
}

actual fun getPlatformCurrentTimeProvider(): TimeProvider = IOSCurrentTimeProvider()

actual object ScreenInfo {
    actual val density: Float
        get() = UIScreen.mainScreen.scale.toFloat()

    actual val densityDpi: Int
        get() {
            // iOS doesn’t expose densityDpi directly,
            // we can approximate:
            val scale = UIScreen.mainScreen.scale
            // Apple uses 163 dpi as "base" (similar to mdpi=160 on Android)
            return (163 * scale).toInt()
        }

    @OptIn(ExperimentalForeignApi::class)
    actual val widthPixels: Int
        get() = CGRectGetWidth(UIScreen.mainScreen.bounds).toInt()
}