package network.bisq.mobile.client.httpclient

import kotlinx.serialization.Serializable
import network.bisq.mobile.i18n.i18n

@Serializable
enum class NetworkType(private val i18nKey: String) {
    LAN("mobile.trustedNodeSetup.networkType.lan"),
    TOR("mobile.trustedNodeSetup.networkType.tor");

    val displayString: String get() = i18nKey.i18n()
}