package network.bisq.mobile.data.utils

object AppUpdateUrls {
    const val GITHUB_RELEASES = "https://github.com/bisq-network/bisq-mobile/releases"

    const val BISQ_CONNECT_IOS_INSTALL_PAGE =
        "https://bisq-network.github.io/bisq-mobile/"

    /** Prefer this on Android so the Play Store app opens directly when present. */
    fun playStoreMarketUrl(packageName: String): String = "market://details?id=$packageName"

    /** HTTPS listing — used when [playStoreMarketUrl] cannot be handled. */
    fun playStoreDetailsUrl(packageName: String): String = "https://play.google.com/store/apps/details?id=$packageName"
}
