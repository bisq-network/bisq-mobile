package network.bisq.mobile.domain.utils

import network.bisq.mobile.domain.getPlatformInfo

fun isIOS(): Boolean {
    val platformInfo = getPlatformInfo()
    val isIOS = platformInfo.name.lowercase().contains("ios")
    return isIOS
}
