package network.bisq.mobile.presentation.common.platform_settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class PlatformSettingsManagerImpl : PlatformSettingsManager {
    @Composable
    override fun rememberBatteryOptimizationsLauncher(onResult: (Boolean) -> Unit): RequestLauncher {
        val requestLauncher =
            remember(onResult) {
                object : RequestLauncher {
                    override fun launch() {
                        onResult(true) // not available on iOS
                    }
                }
            }
        return requestLauncher
    }

    override fun isIgnoringBatteryOptimizations(): Boolean = true
}
