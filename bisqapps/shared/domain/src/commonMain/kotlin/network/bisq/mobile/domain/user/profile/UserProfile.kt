package network.bisq.mobile.domain.user.profile

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.network.identity.NetworkId
import network.bisq.mobile.domain.security.pow.ProofOfWork

@Serializable
data class UserProfile(
    val nickName: String,
    val proofOfWork: ProofOfWork,
    val networkId: NetworkId,
    val terms: String,
    val statement: String,
    val avatarVersion: Int,
    val applicationVersion: String,
    val id: String,
    val nym: String,
    val userName: String,
    val pubKeyHash: String,
    val publishDate: Long
)