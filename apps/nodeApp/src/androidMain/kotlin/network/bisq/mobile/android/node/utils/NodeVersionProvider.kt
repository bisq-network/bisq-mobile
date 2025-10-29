package network.bisq.mobile.android.node.utils

import network.bisq.mobile.android.node.BuildNodeConfig
import network.bisq.mobile.domain.utils.VersionProvider
import network.bisq.mobile.i18n.i18n

class NodeVersionProvider() : VersionProvider {
    private fun getAppName(isDemo: Boolean): String {
        val demo = if (isDemo) "-demo-" else ""
        return demo + BuildNodeConfig.APP_NAME
    }

    override fun getVersionInfo(isDemo: Boolean, isIOS: Boolean): String {
        val appName = getAppName(isDemo)
        return "mobile.resources.versionDetails.node".i18n(
            appName,
            BuildNodeConfig.APP_VERSION,
            BuildNodeConfig.TOR_VERSION,
            BuildNodeConfig.BISQ_CORE_VERSION,
        )
    }

    override fun getAppNameAndVersion(isDemo: Boolean, isIOS: Boolean): String {
        val appName = getAppName(isDemo)
        return "$appName v${BuildNodeConfig.APP_VERSION}"
    }
}