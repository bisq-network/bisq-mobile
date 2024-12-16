package network.bisq.mobile.client.replicated_model.user.profile

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.replicated_model.network.identity.NetworkId
import network.bisq.mobile.client.replicated_model.security.pow.ProofOfWork

@Serializable
data class UserProfile(
    val nickName: String? = null,
    val proofOfWork: ProofOfWork? = null,
    val networkId: NetworkId? = null,
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