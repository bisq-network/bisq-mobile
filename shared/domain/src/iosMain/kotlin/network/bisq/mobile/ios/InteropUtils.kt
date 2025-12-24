@file:OptIn(ExperimentalForeignApi::class)

package network.bisq.mobile.ios

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.DeferScope
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryGetValue
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFErrorRefVar
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFRetain
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.kCFBooleanFalse
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.create
import platform.posix.memcpy
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.createCleaner

fun NSData.toByteArray(): ByteArray {
    if (length > Int.MAX_VALUE.toULong()) throw IndexOutOfBoundsException("length is too large")
    return ByteArray(length.toInt()).apply {
        if (length > 0uL) {
            usePinned {
                memcpy(it.addressOf(0), bytes, length)
            }
        }
    }
}

@OptIn(BetaInteropApi::class)
fun ByteArray.toNSData(): NSData =
    memScoped {
        NSData.create(bytes = allocArrayOf(this@toNSData), length = this@toNSData.size.toULong())
    }

fun NSError.toNiceString(): String {
    val sb = StringBuilder("[${if (domain != null) "$domain error, " else ""}code $code] $localizedDescription\n")
    localizedFailureReason?.let { sb.append("Because: $it") }
    localizedRecoverySuggestion?.let { sb.append("Try: $it") }
    localizedRecoveryOptions?.let { sb.append("Try also:\n - ${it.joinToString("\n - ")}\n") }
    return sb.toString()
}

class CoreFoundationException(
    val nsError: NSError,
) : Throwable(nsError.toNiceString())

class CoreCall private constructor(
    val error: CPointer<CFErrorRefVar>,
    @PublishedApi internal val memScope: MemScope,
) {
    /** Produce a Core Foundation reference whose lifetime is equal to that of the CoreCall */
    inline fun <reified T : CFTypeRef?> giveToCF(v: Any?) = memScope.giveToCF<T>(v)

    /** Helper for calling Core Foundation functions, and bridging exceptions across.
     *
     * Usage:
     * ```
     * CoreCall { SomeCoreFoundationFunction(arg1, arg2, ..., error) }
     * ```
     * `error` is provided by the implicit receiver object, and will be mapped to a
     * `CoreFoundationException` if an error occurs.
     */
    companion object Companion {
        @OptIn(BetaInteropApi::class)
        operator fun <T> invoke(call: CoreCall.() -> T?): T {
            memScoped {
                val errorH = alloc<CFErrorRefVar>()
                val result = CoreCall(errorH.ptr, this@memScoped).call()
                val error = errorH.value
                when {
                    (result != null) && (error == null) -> return result
                    (result == null) && (error != null) ->
                        throw CoreFoundationException(error.takeFromCF<NSError>())
                    else -> throw IllegalStateException("Invalid state returned by Core Foundation call")
                }
            }
        }
    }
}

class SwiftException(
    message: String,
) : Throwable(message)

class SwiftCall private constructor(
    val error: CPointer<ObjCObjectVar<NSError?>>,
) {
    /** Helper for calling swift-objc-mapped functions, and bridging exceptions across.
     *
     * Usage:
     * ```
     * swiftcall { SwiftObj.func(arg1, arg2, .., argN, error) }
     * ```
     * `error` is provided by the implicit receiver object, and will be mapped to a
     * `SwiftException` if the swift call throws.
     */
    companion object Companion {
        @OptIn(BetaInteropApi::class)
        operator fun <T> invoke(call: SwiftCall.() -> T?): T {
            memScoped {
                val errorH = alloc<ObjCObjectVar<NSError?>>()
                val result = SwiftCall(errorH.ptr).call()
                val error = errorH.value
                when {
                    (result != null) && (error == null) -> return result
                    (result == null) && (error != null) -> throw SwiftException(error.toNiceString())
                    else -> throw IllegalStateException("Invalid state returned by Swift")
                }
            }
        }
    }
}

@OptIn(ExperimentalNativeApi::class)
class OwnedCFValue<T : CFTypeRef> constructor(
    val value: T,
) {
    @Suppress("UNUSED")
    private val cleaner = createCleaner(value, ::CFRelease)
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T : CFTypeRef> T.manage() = OwnedCFValue(this)

/** Produce a Core Foundation reference whose lifetime is that of the containing [DeferScope] */
inline fun <reified T : CFTypeRef?> DeferScope.giveToCF(v: Any?) =
    when (v) {
        null -> v
        is Boolean -> if (v) kCFBooleanTrue else kCFBooleanFalse
        is CValuesRef<*> -> v
        else -> CFBridgingRetain(v).also { ref -> this@giveToCF.defer { CFRelease(ref) } }
    } as T

inline fun <reified T> CFTypeRef?.takeFromCF() = CFBridgingRelease(this) as T

fun DeferScope.cfDictionaryOf(vararg pairs: Pair<*, *>): CFDictionaryRef {
    val dict =
        CFDictionaryCreateMutable(
            null,
            pairs.size.toLong(),
            kCFTypeDictionaryKeyCallBacks.ptr,
            kCFTypeDictionaryValueCallBacks.ptr,
        )!!
    defer { CFRelease(dict) } // free it after the memscope finishes
    pairs.forEach { (k, v) -> dict[k] = v }
    return dict
}

class CFDictionaryInitScope private constructor() {
    private val pairs = mutableListOf<Pair<*, *>>()

    fun map(pair: Pair<*, *>) {
        pairs.add(pair)
    }

    infix fun Any?.mapsTo(other: Any?) {
        map(this to other)
    }

    companion object {
        fun resolve(
            scope: DeferScope,
            fn: CFDictionaryInitScope.() -> Unit,
        ) = scope.cfDictionaryOf(*CFDictionaryInitScope().apply(fn).pairs.toTypedArray())
    }
}

fun DeferScope.createCFDictionary(pairs: CFDictionaryInitScope.() -> Unit) = CFDictionaryInitScope.resolve(this, pairs)

inline operator fun <reified T> CFDictionaryRef.get(key: Any?): T =
    memScoped {
        val raw =
            CFDictionaryGetValue(this@get, giveToCF(key))
                ?: throw NoSuchElementException("Key $key not found in CFDictionary")
        CFRetain(raw)
        raw.takeFromCF<T>()
    }

@Suppress("NOTHING_TO_INLINE")
inline operator fun CFMutableDictionaryRef.set(
    key: Any?,
    value: Any?,
) = memScoped {
    CFDictionarySetValue(this@set, giveToCF(key), giveToCF(value))
}
