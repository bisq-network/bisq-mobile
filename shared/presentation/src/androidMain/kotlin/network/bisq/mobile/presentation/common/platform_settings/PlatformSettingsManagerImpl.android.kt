package network.bisq.mobile.presentation.common.platform_settings

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class PlatformSettingsManagerImpl(
    private val context: Context,
) : PlatformSettingsManager {
    @Composable
    override fun rememberBatteryOptimizationsLauncher(onResult: (Boolean) -> Unit): RequestLauncher {
        val launcher =
            rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult(),
            ) {
                onResult(isIgnoringBatteryOptimizations())
            }
        val requestLauncher =
            remember(launcher) {
                object : RequestLauncher {
                    override fun launch() {
                        launcher.launch(
                            Intent().setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
                        )
                    }
                }
            }
        return requestLauncher
    }

    override fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName)
            ?: true // won't happen, but just in case PowerManager is not there, we want to avoid annoying user
    }
}
