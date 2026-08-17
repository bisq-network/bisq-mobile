@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package network.bisq.mobile.data.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import network.bisq.mobile.domain.model.PlatformInfo
import org.koin.core.scope.Scope
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

interface UrlLauncher {
    suspend fun openUrl(url: String): Boolean
}

interface AppUpdateLinker {
    fun getUpdateUrl(): String
}

/**
 * Locale dependent date and time of [epochMillis] in [timeZoneId].
 *
 * Formatting is a platform concern here because the shared code cannot use kotlinx-datetime: it
 * maps to java.time, which needs API 26, while clientApp has minSdk 24 and no desugaring. See
 * [network.bisq.mobile.domain.utils.DateUtils].
 *
 * @param timeZoneId IANA zone id, or null for the system default zone
 */
expect fun formatDateTime(
    epochMillis: Long,
    timeZoneId: String?,
): String

/**
 * Date and time of [epochMillis] in [timeZoneId] as `MMM d, yyyy  HH:mm[:ss]` with English month
 * abbreviations, independent of the device locale. The double space is intentional.
 *
 * @param timeZoneId IANA zone id, or null for the system default zone
 */
expect fun formatMediumDateTime(
    epochMillis: Long,
    timeZoneId: String?,
    includeSeconds: Boolean,
): String

expect fun encodeURIParam(param: String): String

expect fun setupUncaughtExceptionHandler(onCrash: (Throwable) -> Unit)

expect fun getDeviceLanguageCode(): String

expect fun getPlatformInfo(): PlatformInfo

expect fun loadProperties(fileName: String): Map<String, String>

@Serializable(with = PlatformImageSerializer::class)
expect class PlatformImage {
    companion object {
        fun deserialize(data: ByteArray): PlatformImage
    }

    fun serialize(): ByteArray
}

expect fun createEmptyImage(): PlatformImage

object PlatformImageSerializer : KSerializer<PlatformImage> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("PlatformImage", PrimitiveKind.STRING)

    @OptIn(ExperimentalEncodingApi::class)
    override fun serialize(
        encoder: Encoder,
        value: PlatformImage,
    ) {
        val byteArray = value.serialize()
        encoder.encodeString(Base64.encode(byteArray))
    }

    @OptIn(ExperimentalEncodingApi::class)
    override fun deserialize(decoder: Decoder): PlatformImage {
        val byteArray = Base64.decode(decoder.decodeString())
        return PlatformImage.deserialize(byteArray)
    }
}

interface DecimalFormatter {
    fun format(
        value: Double,
        precision: Int,
        useGrouping: Boolean = true,
    ): String
}

expect val decimalFormatter: DecimalFormatter

expect fun setDefaultLocale(language: String)

expect fun getDecimalSeparator(): Char

expect fun getGroupingSeparator(): Char

expect fun String.toDoubleOrNullLocaleAware(): Double?

/**
 * Returns the localized display name for the given ISO 4217 currency code using the current default locale.
 * Must never throw; return currencyCode if unavailable/unknown so callers can fall back.
 */
expect fun getLocaleCurrencyName(currencyCode: String): String

// Scope is Koin's scope
// This is to allow getting androidContext in implementation

/**
 * careful to only call this inside a Koin context on android
 */
expect fun Scope.getStorageDir(): String
