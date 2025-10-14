@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package network.bisq.mobile.presentation

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import network.bisq.mobile.presentation.ui.helpers.IOSCurrentTimeProvider
import network.bisq.mobile.presentation.ui.helpers.TimeProvider
import platform.CoreGraphics.CGRectGetWidth
import platform.Foundation.NSData
import platform.Foundation.getBytes
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

actual fun moveAppToBackground(view: Any?) {
    // NSSelectorFromString("suspend") is a private API, which violates App Store guidelines
    // UIApplication.sharedApplication.performSelector(NSSelectorFromString("suspend"))
    // iOS apps are not allowed to programmatically go Home.
}

actual fun getPlatformCurrentTimeProvider(): TimeProvider = IOSCurrentTimeProvider()

@OptIn(ExperimentalForeignApi::class)
actual fun getScreenWidthDp(): Int {
    return CGRectGetWidth(UIScreen.mainScreen.bounds).toInt()
}

actual fun isAffectedBottomSheetDevice(): Boolean = false
