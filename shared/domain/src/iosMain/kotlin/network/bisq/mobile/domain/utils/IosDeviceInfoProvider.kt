package network.bisq.mobile.domain.utils

import kotlinx.cinterop.ExperimentalForeignApi
import network.bisq.mobile.i18n.i18n
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSNumber
import platform.UIKit.UIDevice
import kotlin.math.pow
import kotlin.math.round

class IosDeviceInfoProvider(
) : DeviceInfoProvider, Logging {
    @OptIn(ExperimentalForeignApi::class)
    override fun getDeviceInfo(): String {
        val na = "N/A"

        try {
            // Device info
            val device = UIDevice.Companion.currentDevice
            val model = device.model
            val systemVersion = device.systemVersion

            // TODO iOS does not provide available RAM easily so we drop it for now. If anyone want to pick that up if not too much effort
            //  would be nice to have.

            // Storage info
            val home = NSHomeDirectory()
            val fileManager = NSFileManager.Companion.defaultManager
            val attrs = try {
                fileManager.attributesOfFileSystemForPath(home, null) ?: emptyMap<Any?, Any?>()
            } catch (e: Exception) {
                emptyMap()
            }

            val totalStorageBytes = (attrs["NSFileSystemSize"] as? NSNumber)?.doubleValue ?: 0.0
            val availStorageBytes = (attrs["NSFileSystemFreeSize"] as? NSNumber)?.doubleValue ?: 0.0
            val totalStorage = formatBytesPrecise(totalStorageBytes.toLong())
            val availStorage = formatBytesPrecise(availStorageBytes.toLong())

            // Battery info
            val batteryLevel = try {
                device.setBatteryMonitoringEnabled(true)
                if (device.batteryLevel >= 0.0) {
                    (device.batteryLevel * 100).toInt().toString() + "%"
                } else {
                    na
                }
            } catch (e: Exception) {
                log.e(e) { "Failed to get battery info" }
                na
            }
            return "mobile.resources.deviceInfo.ios".i18n(
                model,
                systemVersion,
                availStorage, totalStorage,
                batteryLevel
            )
        } catch (e: Exception) {
            log.e(e) { "Failed to get device info" }
            return na
        }
    }

    fun formatBytesPrecise(bytes: Long, decimals: Int = 2): String {
        if (bytes < 1024) return "$bytes B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
        var value = bytes.toDouble()
        var unitIndex = 0
        while (value >= 1024 && unitIndex < units.size - 1) {
            value /= 1024
            unitIndex++
        }
        val multiplier = 10.0.pow(decimals)
        val rounded = round(value * multiplier) / multiplier
        return "$rounded ${units[unitIndex]}"
    }
}