package network.bisq.mobile.domain.utils

interface DeviceInfoProvider {
    fun getDeviceInfo(): String
}