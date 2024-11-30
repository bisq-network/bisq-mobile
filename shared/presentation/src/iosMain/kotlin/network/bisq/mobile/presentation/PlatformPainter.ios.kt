package network.bisq.mobile.presentation

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGContextRef
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetCurrentContext
import platform.UIKit.UIImage
import platform.UIKit.UIScreen

@OptIn(ExperimentalForeignApi::class)
actual fun getPlatformPainter(platformImage: UIImage): Painter {
    return object : Painter() {
        override val intrinsicSize: Size
            get() {
                val size: CValue<CGSize> = platformImage.size
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
                platformImage.drawInRect(
                    CGRectMake(0.0, 0.0, size.width.toDouble(), size.height.toDouble())
                )
            }
            UIGraphicsEndImageContext()
        }
    }
}
