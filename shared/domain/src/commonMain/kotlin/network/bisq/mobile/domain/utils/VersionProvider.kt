package network.bisq.mobile.domain.utils

interface VersionProvider {
    fun getVersionInfo(isDemo: Boolean, isIOS: Boolean): String
}