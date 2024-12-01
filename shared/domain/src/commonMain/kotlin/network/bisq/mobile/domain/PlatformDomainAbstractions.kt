@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package network.bisq.mobile.domain

import com.russhwolf.settings.Settings

expect fun getPlatformSettings(): Settings

interface PlatformInfo {
    val name: String
}

expect fun getPlatformInfo(): PlatformInfo

expect class PlatformImage
