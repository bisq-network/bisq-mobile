package network.bisq.mobile.presentation.common.share

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import network.bisq.mobile.domain.utils.getLogger
import network.bisq.mobile.domain.utils.toNSData
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.stringByAppendingPathComponent
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UINavigationController
import platform.UIKit.UITabBarController
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosShareFileService : ShareFileService {
    private val log = getLogger("IosShareFileService")

    override fun shareUtf8TextFile(
        content: String,
        fileName: String,
    ): Result<Unit> =
        try {
            val base = NSTemporaryDirectory()
            val pathStr = (base as NSString).stringByAppendingPathComponent(fileName)
            val data = content.encodeToByteArray().toNSData()
            data.writeToFile(pathStr, atomically = true)

            val fileUrl = platform.Foundation.NSURL.fileURLWithPath(pathStr)
            val activityItems = listOf(fileUrl)
            val controller = UIActivityViewController(activityItems = activityItems, applicationActivities = null)

            val root = findTopViewController()
            if (root == null) {
                log.e { "No root view controller for share sheet" }
                return Result.failure(IllegalStateException("No root view controller"))
            }
            // UIPopover anchor: UIActivityViewController.popoverPresentationController is not
            // exposed in Kotlin/Native UIKit bindings here; sheet still works on iPhone.
            root.presentViewController(controller, animated = true, completion = null)
            Result.success(Unit)
        } catch (e: Throwable) {
            log.e(e) { "Failed to share file" }
            Result.failure(e as? Exception ?: Exception(e.message))
        }

    private fun findTopViewController(): UIViewController? {
        val root =
            try {
                @Suppress("DEPRECATION")
                UIApplication.sharedApplication.keyWindow?.rootViewController
            } catch (_: Exception) {
                try {
                    UIApplication.sharedApplication.connectedScenes
                        .toList()
                        .filterIsInstance<UIWindowScene>()
                        .firstNotNullOfOrNull { scene ->
                            scene
                                .windows
                                .toList()
                                .filterIsInstance<UIWindow>()
                                .firstOrNull { it.keyWindow }
                                ?.rootViewController
                        }
                } catch (_: Exception) {
                    null
                }
            } ?: UIApplication.sharedApplication.delegate
                ?.window
                ?.rootViewController
        var top = root ?: return null
        while (true) {
            when {
                top.presentedViewController != null ->
                    top = top.presentedViewController!!
                top is UINavigationController ->
                    top = (top as UINavigationController).visibleViewController ?: break
                top is UITabBarController ->
                    top = (top as UITabBarController).selectedViewController ?: break
                else -> break
            }
        }
        return top
    }
}
