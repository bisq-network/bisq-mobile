package network.bisq.mobile.domain.data.replicated.identity

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.domain.data.replicated.security.keys.KeyBundleVO

@Serializable
data class IdentityVO(val tag: String, val networkId: NetworkIdVO, val keyBundle: KeyBundleVO)