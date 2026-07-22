package network.bisq.mobile.data.utils

object AppUpdateUrls {
    const val GITHUB_RELEASES = "https://github.com/bisq-network/bisq-mobile/releases"

    const val BISQ_CONNECT_IOS_INSTALL_PAGE =
        "https://bisq-network.github.io/bisq-mobile/"

    fun playStoreDetailsUrl(packageName: String): String = "https://play.google.com/store/apps/details?id=$packageName"
}
