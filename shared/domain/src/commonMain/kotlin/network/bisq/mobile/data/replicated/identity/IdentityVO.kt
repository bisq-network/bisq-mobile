package network.bisq.mobile.data.replicated.identity

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.data.replicated.security.keys.KeyBundleVO

@Serializable
data class IdentityVO(
    val tag: String,
    val networkId: NetworkIdVO,
    val keyBundle: KeyBundleVO,
)

val identitiesDemoObj = listOf("id1", "id2")
