package network.bisq.mobile.domain.replicated.user.profile

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val nickName: String? = null,
    val proofOfWork: network.bisq.mobile.domain.replicated.security.pow.ProofOfWork? = null,
    val networkId: network.bisq.mobile.domain.replicated.network.identity.NetworkId? = null,
    val terms: String? = null,
    val statement: String? = null,
    val avatarVersion: Int? = null,
    val applicationVersion: String? = null,
    val id: String? = null,
    val nym: String? = null,
    val userName: String? = null,
    val pubKeyHash: String? = null,
    val publishDate: Long? = null
)