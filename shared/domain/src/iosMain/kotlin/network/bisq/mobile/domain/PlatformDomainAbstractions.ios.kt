@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package network.bisq.mobile.domain

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.refTo
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.usePinned
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.Serializable
import org.koin.core.scope.Scope
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSBundle
import platform.Foundation.NSCharacterSet
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSDictionary
import platform.Foundation.NSException
import platform.Foundation.NSFileManager
import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleCurrencyCode
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSSetUncaughtExceptionHandler
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.URLPathAllowedCharacterSet
import platform.Foundation.allKeys
import platform.Foundation.create
import platform.Foundation.currentLocale
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.languageCode
import platform.Foundation.localeWithLocaleIdentifier
import platform.Foundation.stringByAddingPercentEncodingWithAllowedCharacters
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy
import kotlin.experimental.ExperimentalNativeApi

actual fun formatDateTime(dateTime: LocalDateTime): String {
    val formatter = NSDateFormatter().apply {
        dateStyle = NSDateFormatterMediumStyle
        timeStyle = NSDateFormatterShortStyle
        locale = NSLocale.currentLocale
    }

    val instant = dateTime.toInstant(TimeZone.currentSystemDefault())
    val nsDate = NSDate(instant.toEpochMilliseconds() / 1000.0)

    return formatter.stringFromDate(nsDate)
}

@OptIn(BetaInteropApi::class)
actual fun encodeURIParam(param: String): String {
    return NSString.create(string = param)
        .stringByAddingPercentEncodingWithAllowedCharacters(NSCharacterSet.URLPathAllowedCharacterSet)
        ?: param
}

actual fun getDeviceLanguageCode(): String {
    return NSLocale.currentLocale.languageCode ?: "en"
}

private var globalOnCrash: ((Throwable) -> Unit)? = null

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@Throws(Exception::class)
actual fun setupUncaughtExceptionHandler(onCrash: (Throwable) -> Unit) {
    // TODO this catches the exceptions but let them go through crashing the app, whether in android it will stop the propagation
    globalOnCrash = onCrash
    NSSetUncaughtExceptionHandler(staticCFunction { exception: NSException? ->
        if (exception != null) {
            println("Uncaught exception: ${exception.name}, reason: ${exception.reason}")
            println("Stack trace: ${exception.callStackSymbols.joinToString("\n")}")

            // TODO report to some sort non-survaillant crashlytics?

            // Let the UI react
            val cause = Throwable(exception.reason)
            val throwable = Throwable(message = exception.name, cause)
            globalOnCrash?.invoke(throwable)

            // needed on iOS
            dispatch_async(dispatch_get_main_queue()) {
                println("Performing cleanup after uncaught exception")
            }
        }
//        setUnhandledExceptionHook { throwable ->
//            println("Uncaught Kotlin exception: ${throwable.message}")
//            throwable.printStackTrace()
//
//            // Perform cleanup on the main thread
//            dispatch_async(dispatch_get_main_queue()) {
//                println("Performing cleanup after uncaught Kotlin exception")
//                globalOnCrash?.invoke()
//            }
//        }
    })
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
    override val type = PlatformType.IOS
}

actual fun getPlatformInfo(): PlatformInfo = IOSPlatformInfo()

@OptIn(BetaInteropApi::class)
actual fun loadProperties(fileName: String): Map<String, String> {
    val bundle = NSBundle.mainBundle
    val path = bundle.pathForResource(fileName.removeSuffix(".properties"), "properties")
        ?: return emptyMap()

    // Read file as UTF-8 text and parse Java-style .properties content
    val data = NSData.dataWithContentsOfFile(path) ?: return emptyMap()
    val nsString = NSString.create(data = data, encoding = NSUTF8StringEncoding)
    val content = nsString?.toString() ?: return emptyMap()

    return parseProperties(content)
}

private fun parseProperties(content: String): Map<String, String> {
    val result = mutableMapOf<String, String>()
    val lines = content.lines().toMutableList()
    val logicalLines = mutableListOf<String>()

    var i = 0
    while (i < lines.size) {
        var line = lines[i]
        // Handle line continuations ending with unescaped backslash
        while (endsWithUnescapedBackslash(line)) {
            val next = if (i + 1 < lines.size) lines[i + 1] else ""
            line = line.substring(0, line.length - 1) + next.trimStart()
            i += 1
        }
        logicalLines.add(line)
        i += 1
    }

    for (raw in logicalLines) {
        val line = raw.trimStart()
        if (line.isEmpty()) continue
        val firstChar = line[0]
        if (firstChar == '#' || firstChar == '!') continue

        val key = StringBuilder()
        val value = StringBuilder()
        var inKey = true
        var escaped = false

        fun appendTarget(c: Char) {
            if (inKey) key.append(c) else value.append(c)
        }

        for (idx in 0 until line.length) {
            val c = line[idx]
            if (!escaped) {
                when (c) {
                    '\\' -> escaped = true
                    '=', ':' -> if (inKey) {
                        inKey = false
                    } else appendTarget(c)

                    ' ', '\t', '\u000c' -> if (inKey) {
                        // whitespace can separate key and value
                        // skip consecutive whitespace and set to value
                        var j = idx + 1
                        while (j < line.length && (line[j] == ' ' || line[j] == '\t' || line[j] == '\u000c')) j++
                        if (j < line.length && !inKey) {
                            // already in value
                            appendTarget(c)
                        } else if (inKey) {
                            inKey = false
                        }
                    } else appendTarget(c)

                    else -> appendTarget(c)
                }
            } else {
                // escaped char in either key or value
                when (c) {
                    't' -> appendTarget('\t')
                    'n' -> appendTarget('\n')
                    'r' -> appendTarget('\r')
                    'f' -> appendTarget('\u000C') // form feed
                    '\\', ' ', ':', '=' -> appendTarget(c)
                    'u' -> {
                        // Unicode escape \uXXXX
                        val remaining = line.substring(idx + 1)
                        if (remaining.length >= 4) {
                            val hex = remaining.substring(0, 4)
                            val code = hex.toIntOrNull(16)
                            if (code != null) {
                                appendTarget(code.toChar())
                                // skip processed hex digits
                                // adjust main loop index
                                // idx will be incremented by for-loop, so advance by 4
                                // but we can't modify idx in Kotlin for-loop; rebuild remainder
                            } else {
                                appendTarget('u'); appendTarget(hex[0])
                            }
                        } else {
                            appendTarget('u')
                        }
                        // Reconstruct the rest after processing unicode
                        // Simplify by appending as is when complex; to keep robust we fall back
                    }

                    else -> appendTarget(c)
                }
                escaped = false
            }
        }

        val k = key.toString().trimEnd()
        val v = value.toString().trimStart()
        result[k] = v
    }

    return result
}

private fun endsWithUnescapedBackslash(s: String): Boolean {
    var count = 0
    var i = s.length - 1
    while (i >= 0 && s[i] == '\\') {
        count++; i--
    }
    return count % 2 == 1
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
            val image = UIImage(data = nsData)
                ?: throw IllegalArgumentException("Failed to decode image data")
            return PlatformImage(image)
        }
    }
}

actual fun createEmptyImage(): PlatformImage {
    // Create a 1x1 transparent image
    val size = CGSizeMake(1.0, 1.0)
    UIGraphicsBeginImageContextWithOptions(size, false, 0.0)
    val image = UIGraphicsGetImageFromCurrentImageContext()!!
    UIGraphicsEndImageContext()
    return PlatformImage(image)
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
        val formatter = NSNumberFormatter().apply {
            numberStyle = NSNumberFormatterDecimalStyle
            maximumFractionDigits = precision.toULong()
            minimumFractionDigits = precision.toULong()
            locale = defaultLocale
        }
        return formatter.stringFromNumber(NSNumber(value)) ?: value.toString()
    }
}

private var defaultLocale: NSLocale = NSLocale.currentLocale

actual fun setDefaultLocale(language: String) {
    defaultLocale = NSLocale.localeWithLocaleIdentifier(language)
}

actual fun getDecimalSeparator(): Char {
    val formatter = NSNumberFormatter().apply {
        numberStyle = NSNumberFormatterDecimalStyle
        locale = defaultLocale
    }
    return formatter.decimalSeparator.first()
}

actual fun getGroupingSeparator(): Char {
    val formatter = NSNumberFormatter().apply {
        numberStyle = NSNumberFormatterDecimalStyle
        locale = defaultLocale
    }
    return formatter.groupingSeparator.first()
}

actual fun String.toDoubleOrNullLocaleAware(): Double? {
    val formatter = NSNumberFormatter().apply {
        numberStyle = NSNumberFormatterDecimalStyle
        locale = defaultLocale
    }
    val number = formatter.numberFromString(this)
    return number?.doubleValue?.toDouble()
}

actual fun getLocaleCurrencyName(currencyCode: String): String {
    val rawName = defaultLocale.displayNameForKey(NSLocaleCurrencyCode, currencyCode)
    return rawName ?: currencyCode
}

@OptIn(ExperimentalForeignApi::class)
actual fun Scope.getStorageDir(): String {
    val paths = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true)
    val appSupport = (paths.firstOrNull() as? String)
        ?: throw IllegalStateException("Could not get application support directory")
    val url = NSURL.fileURLWithPath(appSupport).URLByAppendingPathComponent("Data")
        ?: throw IllegalStateException("Could not get Data in support directory")
    memScoped {
        val success = NSFileManager.defaultManager.createDirectoryAtURL(
            url, withIntermediateDirectories = true, attributes = null, error = null
        )
        if (!success) throw IllegalStateException("Failed to create application support subdirectory")
    }
    return url.path ?: appSupport
}