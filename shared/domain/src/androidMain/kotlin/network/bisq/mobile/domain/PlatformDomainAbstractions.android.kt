@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package network.bisq.mobile.domain

import android.os.Build
import com.russhwolf.settings.Settings

actual fun getPlatformSettings(): Settings {
    return Settings()
}

class AndroidPlatformInfo : PlatformInfo {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatformInfo(): PlatformInfo = AndroidPlatformInfo()

// FIXME if ImageBitmap is used it gives a compile error
actual typealias PlatformImage = Any