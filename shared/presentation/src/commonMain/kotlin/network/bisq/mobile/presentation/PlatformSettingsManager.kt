package network.bisq.mobile.presentation

import androidx.compose.runtime.Composable

interface RequestLauncher {
    fun launch()
}

interface PlatformSettingsManager {
    @Composable
    fun rememberBatteryOptimizationsLauncher(onResult: (Boolean) -> Unit): RequestLauncher

    fun isIgnoringBatteryOptimizations(): Boolean
}