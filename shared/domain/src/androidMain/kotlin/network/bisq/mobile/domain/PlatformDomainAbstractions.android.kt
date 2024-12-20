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
import network.bisq.mobile.i18n.I18nSupport
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.Scanner


actual fun getPlatformSettings(): Settings {
    return Settings()
}

class AndroidPlatformInfo : PlatformInfo {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatformInfo(): PlatformInfo = AndroidPlatformInfo()

actual fun loadFromResources(fileName: String): String {
    val classLoader = Thread.currentThread().contextClassLoader
    val resource = classLoader?.getResource(fileName)
        ?: throw IllegalArgumentException("File not found: $fileName")
    val readText = resource.readText()
    return readText
}

fun readStringFromResource(resourceName: String?): String {
    Scanner(getResourceAsStream(resourceName!!)).use { scanner ->
        return readFromScanner(scanner)
    }
}
fun getResourceAsStream(resourceName: String): InputStream {
    val classLoader1 = I18nSupport::class.java.classLoader
    val classLoader = Thread.currentThread().contextClassLoader
    val resource = classLoader.getResourceAsStream(resourceName)
        ?: throw IOException("Could not load $resourceName")
    return resource
}
fun readFromScanner(scanner: Scanner): String {
    val sb = StringBuilder()
    while (scanner.hasNextLine()) {
        sb.append(scanner.nextLine())
        if (scanner.hasNextLine()) {
            sb.append(System.lineSeparator())
        }
    }
    return sb.toString()
}

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