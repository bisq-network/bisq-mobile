package network.bisq.mobile.domain.utils

import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.i18n.i18n

class ClientVersionProvider() : VersionProvider {
    override fun getVersionInfo(isDemo: Boolean, isIOS: Boolean): String {
        val demo = if (isDemo) "-demo-" else ""
        val appVersion = demo + if (isIOS) BuildConfig.IOS_APP_VERSION else BuildConfig.ANDROID_APP_VERSION
        return "mobile.resources.versionDetails.client".i18n(BuildConfig.APP_NAME, appVersion, BuildConfig.BISQ_API_VERSION)
    }

    override fun getAppNameAndVersion(isDemo: Boolean, isIOS: Boolean): String {
        val demo = if (isDemo) "-demo-" else ""
        val appVersion = demo + if (isIOS) BuildConfig.IOS_APP_VERSION else BuildConfig.ANDROID_APP_VERSION
        return "${BuildConfig.APP_NAME} v${appVersion}"
    }
}