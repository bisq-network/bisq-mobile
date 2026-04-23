package network.bisq.mobile.presentation.common.share

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import network.bisq.mobile.domain.utils.getLogger
import network.bisq.mobile.domain.utils.toNSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.setValue
import platform.Foundation.stringByAppendingPathComponent
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice
import platform.UIKit.UINavigationController
import platform.UIKit.UIPopoverArrowDirectionAny
import platform.UIKit.UITabBarController
import platform.UIKit.UIUserInterfaceIdiomPad
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosShareFileService : ShareFileService {
    private val log = getLogger("IosShareFileService")

    override suspend fun shareUtf8TextFile(
        content: String,
        fileName: String,
    ): Result<Unit> =
        try {
            val writePrepared =
                withContext(Dispatchers.Default) {
                    val base = NSTemporaryDirectory()
                    val path = (base as NSString).stringByAppendingPathComponent(fileName)
                    val utf8Bytes = content.encodeToByteArray()
                    val data = utf8Bytes.toNSData()
                    val written = data.writeToFile(path, atomically = true)
                    if (!written) {
                        val message =
                            "Failed to write temporary share file (path=$path, " +
                                "contentUtf8Bytes=${utf8Bytes.size}, charCount=${content.length}, fileName=$fileName)"
                        log.e { message }
                        return@withContext Result.failure(IllegalStateException(message))
                    }
                    Result.success(path)
                }

            writePrepared.fold(
                onSuccess = { pathStr ->
                    withContext(Dispatchers.Main) {
                        val fileUrl = platform.Foundation.NSURL.fileURLWithPath(pathStr)
                        val activityItems = listOf(fileUrl)
                        val controller = UIActivityViewController(activityItems = activityItems, applicationActivities = null)
                        val pathToRemove = pathStr
                        controller.completionWithItemsHandler = { _, _, _, _ ->
                            try {
                                if (!NSFileManager.defaultManager.removeItemAtPath(pathToRemove, null)) {
                                    log.e { "Failed to remove temporary share file: $pathToRemove" }
                                }
                            } catch (e: Throwable) {
                                log.e(e) { "Failed to remove temporary share file: $pathToRemove" }
                            }
                        }

                        val root = findTopViewController()
                        if (root == null) {
                            log.e { "No root view controller for share sheet" }
                            NSFileManager.defaultManager.removeItemAtPath(pathToRemove, null)
                            return@withContext Result.failure(IllegalStateException("No root view controller"))
                        }
                        configureShareSheetPopoverAnchorIfPad(controller, root)
                        root.presentViewController(controller, animated = true, completion = null)
                        Result.success(Unit)
                    }
                },
                onFailure = { e -> Result.failure(e as? Exception ?: Exception(e.message)) },
            )
        } catch (e: Throwable) {
            log.e(e) { "Failed to share file" }
            Result.failure(e as? Exception ?: Exception(e.message))
        }

    // Direct popover property access is not in Kotlin/Native UIKit bindings; configure via KVC on key path.
    private fun configureShareSheetPopoverAnchorIfPad(
        controller: UIActivityViewController,
        host: UIViewController,
    ) {
        if (UIDevice.currentDevice.userInterfaceIdiom != UIUserInterfaceIdiomPad) {
            return
        }
        val anchorView = host.view
        controller.setValue(anchorView, forKeyPath = "popoverPresentationController.sourceView")
        controller.setValue(anchorView.bounds, forKeyPath = "popoverPresentationController.sourceRect")
        controller.setValue(UIPopoverArrowDirectionAny, forKeyPath = "popoverPresentationController.permittedArrowDirections")
    }

    private fun rootViewControllerFromConnectedScenes(): UIViewController? =
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
        } catch (e: Exception) {
            log.w(e) { "Failed to resolve root view controller from connected UIWindowScenes" }
            null
        }

    @Suppress("DEPRECATION")
    private fun rootViewControllerFromKeyWindow(): UIViewController? =
        try {
            UIApplication.sharedApplication.keyWindow?.rootViewController
        } catch (e: Exception) {
            log.w(e) { "Failed to resolve root view controller from UIApplication.keyWindow" }
            null
        }

    private fun rootViewControllerFromDelegateWindow(): UIViewController? =
        try {
            UIApplication.sharedApplication.delegate
                ?.window
                ?.rootViewController
        } catch (e: Exception) {
            log.w(e) { "Failed to resolve root view controller from app delegate window" }
            null
        }

    private fun findTopViewController(): UIViewController? {
        val root =
            rootViewControllerFromConnectedScenes()
                ?: rootViewControllerFromKeyWindow()
                ?: rootViewControllerFromDelegateWindow()
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
