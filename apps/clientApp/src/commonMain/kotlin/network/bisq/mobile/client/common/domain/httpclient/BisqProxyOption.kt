package network.bisq.mobile.client.common.domain.httpclient

import kotlinx.serialization.Serializable
import network.bisq.mobile.i18n.i18n

@Serializable
enum class BisqProxyOption(
    private val i18nKey: String,
) {
    NONE("mobile.network.proxy.option.none"),
    INTERNAL_TOR("mobile.network.proxy.option.internal_tor"),
    EXTERNAL_TOR("mobile.network.proxy.option.external_tor"),
    SOCKS_PROXY("mobile.network.proxy.option.socks"),
    ;

    val displayString: String get() = i18nKey.i18n()

    val isTorProxyOption: Boolean get() = this == INTERNAL_TOR || this == EXTERNAL_TOR
}
