package network.bisq.mobile.domain.data.replicated.user.profile

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.domain.data.replicated.security.pow.ProofOfWorkVO

@Serializable
data class UserProfileVO(
    val version: Int,
    val nickName: String,
    val proofOfWork: ProofOfWorkVO,
    val avatarVersion: Int,
    val networkId: NetworkIdVO,
    val terms: String,
    val statement: String,
    val applicationVersion: String,
    val nym: String,
    val userName: String,
    val publishDate: Long
)
