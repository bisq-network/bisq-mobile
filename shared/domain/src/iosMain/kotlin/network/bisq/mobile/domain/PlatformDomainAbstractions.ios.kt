@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package network.bisq.mobile.domain

import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.Settings
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.refTo
import kotlinx.cinterop.usePinned
import kotlinx.serialization.Serializable
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSDictionary
import platform.Foundation.NSLocale
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.allKeys
import platform.Foundation.create
import platform.Foundation.currentLocale
import platform.Foundation.dictionaryWithContentsOfFile
import platform.Foundation.languageCode
import platform.Foundation.stringWithFormat
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.posix.memcpy
import kotlin.collections.set

@OptIn(ExperimentalSettingsImplementation::class)
actual fun getPlatformSettings(): Settings {
    // TODO we might get away just using normal Settings() KMP agnostic implementation,
    // leaving this here to be able to choose the specific one for iOS - defaulting to KeyChain
    return KeychainSettings("Settings")
}

actual fun getDeviceLanguageCode(): String {
    return NSLocale.currentLocale.languageCode ?: "en"
}

class IOSUrlLauncher : UrlLauncher {
    override fun openUrl(url: String) {
        val nsUrl = NSURL.URLWithString(url)
        if (nsUrl != null) {
            // fake secondary parameters are important so that iOS compiler knows which override to use
            UIApplication.sharedApplication.openURL(nsUrl, options = mapOf<Any?, String>(), completionHandler = null)
        }
    }
}

class IOSPlatformInfo : PlatformInfo {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatformInfo(): PlatformInfo = IOSPlatformInfo()

actual fun loadProperties(fileName: String): Map<String, String> {
    val bundle = NSBundle.mainBundle
    /*val path = bundle.pathForResource(fileName.removeSuffix(".properties"), "properties")
        ?: throw IllegalArgumentException("Resource not found: $fileName")*/
    val path = bundle.pathForResource(fileName.removeSuffix(".properties"), "properties")
    // FIXME resources not found yet
    if (path == null) {
        return emptyMap()
    }

    val properties = NSDictionary.dictionaryWithContentsOfFile(path) as NSDictionary?
        ?: throw IllegalStateException("Failed to load properties from $path")

    return properties.entriesAsMap()
}

fun NSDictionary.entriesAsMap(): Map<String, String> {
    val map = mutableMapOf<String, String>()
    val keys = this.allKeys as List<*> // `allKeys` provides a list of keys
    for (key in keys) {
        val keyString = key.toString()
        val valueString = this.objectForKey(key).toString()
        map[keyString] = valueString
    }
    return map
}

@Serializable(with = PlatformImageSerializer::class)
actual class PlatformImage(val image: UIImage) {
    actual fun serialize(): ByteArray {
        val nsData: NSData = UIImagePNGRepresentation(image)!!
        return nsData.toByteArray()
    }

    companion actual object {
        actual fun deserialize(data: ByteArray): PlatformImage {
            val nsData = data.toNSData()
            val image = UIImage(data = nsData)!!
            return PlatformImage(image)
        }
    }
}

// Helper extensions for NSData conversion:
@OptIn(ExperimentalForeignApi::class)
fun NSData.toByteArray(): ByteArray {
    val byteArray = ByteArray(this.length.toInt())
    byteArray.usePinned { pinned ->
        memcpy(pinned.addressOf(0), this.bytes, this.length)
    }
    return byteArray
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
fun ByteArray.toNSData(): NSData {
    return NSData.create(bytes = this.refTo(0).getPointer(MemScope()), length = this.size.toULong())
}

actual val decimalFormatter: DecimalFormatter = object : DecimalFormatter {
    override fun format(value: Double, precision: Int): String {
        val pattern = "%.${precision}f"
        return NSString.stringWithFormat(pattern, value)
    }
}
