package network.bisq.mobile.data.replicated.identity

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import network.bisq.mobile.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.data.replicated.security.keys.KeyBundleVO

@Serializable
data class IdentityVO(
    val tag: String,
    val networkId: NetworkIdVO,
    // @Transient: JSON deserialization skips this field entirely (private key material
    // not needed by mobile for display). Node app sets it directly via constructor.
    @Transient val keyBundle: KeyBundleVO? = null,
)

val identitiesDemoObj = listOf("id1", "id2")
