package network.bisq.mobile.domain.data

import network.bisq.mobile.client.shared.BuildConfig
import platform.Foundation.NSProcessInfo

actual fun provideIsSimulator(): Boolean {
    val deviceModel = NSProcessInfo.processInfo.environment["SIMULATOR_DEVICE_NAME"] as? String
    return deviceModel != null
}

actual fun provideApiHost(): String {
    return BuildConfig.WS_IOS_HOST.takeIf { it.isNotEmpty() } ?: "localhost"
}

actual fun provideApiPort(): Int {
    return (BuildConfig.WS_PORT.takeIf { it.isNotEmpty() } ?: "8090").toInt()
}