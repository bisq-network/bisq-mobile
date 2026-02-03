package network.bisq.mobile.client.common.domain.service.push_notification

import platform.UIKit.UIDevice

/**
 * iOS implementation of getDeviceId().
 * Uses UIDevice.identifierForVendor which:
 * - Is unique per device for apps from the same vendor
 * - Persists across app reinstalls (as long as at least one app from the vendor remains)
 * - Returns the same value for all apps from the same vendor on the same device
 */
actual fun getDeviceId(): String {
    val identifier = UIDevice.currentDevice.identifierForVendor
    return identifier?.UUIDString ?: throw IllegalStateException("Unable to get device identifier")
}
