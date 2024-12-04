package network.bisq.mobile.service

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import network.bisq.mobile.utils.getLogger
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.getBytes
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIScreen

object IosImageUtil {
    @OptIn(ExperimentalForeignApi::class)
    fun composeImage(
        basePath: String,
        paths: Array<String>,
        width: Int,
        height: Int
    ): UIImage? {
        val size = CGSizeMake(width.toDouble(), height.toDouble())
        var resultImage: UIImage? = null
        UIGraphicsBeginImageContextWithOptions(size, false, UIScreen.mainScreen.scale)
        try {
            paths.forEach { path ->
                val image = getImageByPath(basePath, path)
                if (image != null) {
                    val scaledImage = image.scaleToSize(size, width.toDouble(), height.toDouble())
                    scaledImage.drawInRect(
                        CGRectMake(
                            0.0,
                            0.0,
                            width.toDouble(),
                            height.toDouble()
                        )
                    )
                }
            }
            resultImage = UIGraphicsGetImageFromCurrentImageContext()
        } catch (e: Exception) {
            getLogger("IosImageUtil").e("Exception at creating image", e)
        } finally {
            UIGraphicsEndImageContext()
        }
        return resultImage
    }


    fun getImageByPath(path: String): UIImage? {
        val resourcePath = NSBundle.mainBundle.pathForResource(
            name = path,
            ofType = null
        )
        val uiImage = resourcePath?.let { UIImage.imageWithContentsOfFile(it) }
        return uiImage
    }

    private fun getImageByPath(basePath: String, path: String): UIImage? {
        val fullPath = basePath + path
        val imageByPath = getImageByPath(fullPath)
        return imageByPath
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
}

@OptIn(ExperimentalForeignApi::class)
fun UIImage.scaleToSize(size: CValue<CGSize>, width: Double, height: Double): UIImage {
    UIGraphicsBeginImageContextWithOptions(size, false, UIScreen.mainScreen.scale)
    this.drawInRect(CGRectMake(0.0, 0.0, width, height))
    val scaledImage = UIGraphicsGetImageFromCurrentImageContext() ?: this
    UIGraphicsEndImageContext()
    return scaledImage
}
