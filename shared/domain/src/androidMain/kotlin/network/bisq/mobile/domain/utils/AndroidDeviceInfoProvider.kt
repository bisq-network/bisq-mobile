package network.bisq.mobile.domain.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import network.bisq.mobile.i18n.i18n

class AndroidDeviceInfoProvider(
    private var context: Context
) : DeviceInfoProvider, Logging {
    override fun getDeviceInfo(): String {
        // Memory info
        val na = "data.na".i18n()
        var totalMem = na
        var availMem = na
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        activityManager?.let {
            val memInfo = ActivityManager.MemoryInfo()
            it.getMemoryInfo(memInfo)
            totalMem = ByteUnitUtil.formatBytesPrecise(memInfo.totalMem)
            availMem = ByteUnitUtil.formatBytesPrecise(memInfo.availMem)
        }

        // Storage info
        var totalStorage = na
        var availStorage = na
        try {
            val stat = StatFs(Environment.getDataDirectory().absolutePath)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availBlocks = stat.availableBlocksLong
            totalStorage = ByteUnitUtil.formatBytesPrecise(blockSize * totalBlocks)
            availStorage = ByteUnitUtil.formatBytesPrecise(blockSize * availBlocks)
        } catch (e: Exception) {
        }


        // Battery info
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, intentFilter)
        var batteryLevel = na
        batteryStatus?.let { intent ->
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val batteryPct = if (scale > 0) level / scale.toFloat() else 0f
            batteryLevel = (batteryPct * 100).toString() + "%"
        }

        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercaseChar() }
        return "mobile.resources.deviceInfo.android".i18n(
            manufacturer, Build.MODEL,
            Build.VERSION.RELEASE, Build.VERSION.SDK_INT.toString(),
            availMem, totalMem,
            availStorage, totalStorage,
            batteryLevel
        )
    }
}