package network.bisq.mobile.client.common.domain.utils

import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.domain.utils.VersionProvider
import network.bisq.mobile.i18n.i18n

/**
 * @param isTorRouted whether the trusted node is currently reached over Tor. Read at call time,
 * not at construction: the websocket service that knows this depends on the HTTP client service,
 * which depends on this provider, so holding it directly would close a DI cycle.
 */
class ClientVersionProvider(
    private val isTorRouted: () -> Boolean,
) : VersionProvider {
    private fun getAppVersion(
        isDemo: Boolean,
        isIOS: Boolean,
    ): String {
        val demo = if (isDemo) "demo-" else ""
        return demo + if (isIOS) BuildConfig.IOS_APP_VERSION else BuildConfig.ANDROID_APP_VERSION
    }

    override fun getVersionInfo(
        isDemo: Boolean,
        isIOS: Boolean,
    ): String {
        val appVersion = getAppVersion(isDemo, isIOS)
        // The bundled tor daemon only matters when it is the one carrying the connection; a
        // clearnet or LAN node never touches it.
        return if (isTorRouted()) {
            "mobile.resources.versionDetails.client.tor".i18n(
                BuildConfig.APP_NAME,
                appVersion,
                BuildConfig.BISQ_API_VERSION,
                BuildConfig.TOR_VERSION,
            )
        } else {
            "mobile.resources.versionDetails.client".i18n(
                BuildConfig.APP_NAME,
                appVersion,
                BuildConfig.BISQ_API_VERSION,
            )
        }
    }

    override fun getAppNameAndVersion(
        isDemo: Boolean,
        isIOS: Boolean,
    ): String {
        val appVersion = getAppVersion(isDemo, isIOS)
        return "${BuildConfig.APP_NAME} v$appVersion"
    }
}
