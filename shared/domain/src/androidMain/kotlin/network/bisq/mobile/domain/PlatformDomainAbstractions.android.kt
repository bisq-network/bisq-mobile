@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package network.bisq.mobile.domain

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.text.DecimalFormat

actual fun getPlatformSettings(): Settings {
    return Settings()
}

actual fun getDeviceLanguageCode(): String {
    return Locale.getDefault().language
}

class AndroidPlatformInfo : PlatformInfo {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatformInfo(): PlatformInfo = AndroidPlatformInfo()

@Serializable(with = PlatformImageSerializer::class)
actual class PlatformImage(val bitmap: ImageBitmap) {
    actual companion object {
        actual fun deserialize(data: ByteArray): PlatformImage {
            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
            return PlatformImage(bitmap.asImageBitmap())
        }
    }

    actual fun serialize(): ByteArray {
        val androidBitmap = bitmap.asAndroidBitmap()
        val stream = ByteArrayOutputStream()
        androidBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}

actual val decimalFormatter: DecimalFormatter = object : DecimalFormatter {
    private val formatters: MutableMap<Int, DecimalFormat> = mutableMapOf()
    override fun format(value: Double, precision: Int): String {
        formatters.getOrPut(precision) { DecimalFormat(generatePattern(precision)) }
        return formatters[precision]!!.format(value)
    }

    private fun generatePattern(precision: Int): String {
        return if (precision > 0) {
            buildString {
                append("0.")
                repeat(precision) { append("0") }
            }
        } else {
            "0"
        }
    }
}
