package network.bisq.mobile.domain.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Debug
import android.os.Environment
import android.os.StatFs
import network.bisq.mobile.i18n.i18n

class AndroidDeviceInfoProvider(
    private var context: Context
) : DeviceInfoProvider, Logging {

    private var deviceMemInfo: ActivityManager.MemoryInfo = ActivityManager.MemoryInfo()
    private val appMemInfo: Debug.MemoryInfo = Debug.MemoryInfo()
    private val runtime = Runtime.getRuntime()

    override fun getDeviceInfo(): String {
        // Memory info
        val na = "data.na".i18n()
        var deviceUsedMB = na
        var deviceAvailMB = na
        var deviceTotalMB = na
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        activityManager?.let {
            it.getMemoryInfo(deviceMemInfo)
            val totalMem = deviceMemInfo.totalMem
            val availMem = deviceMemInfo.availMem
            val used = totalMem - availMem
            deviceUsedMB = ByteUnitUtil.formatBytesPrecise(used, 0)
            deviceTotalMB = ByteUnitUtil.formatBytesPrecise(totalMem, 0)
            deviceAvailMB = ByteUnitUtil.formatBytesPrecise(availMem, 0)
        }

        // App memory
        Debug.getMemoryInfo(appMemInfo)
        val appUsedMB = ByteUnitUtil.formatBytesPrecise(appMemInfo.totalPss * 1024L, 0)

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
            deviceAvailMB, deviceUsedMB, deviceTotalMB,
            appUsedMB,
            availStorage, totalStorage,
            batteryLevel
        )
    }
}