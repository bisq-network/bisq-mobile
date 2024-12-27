package network.bisq.mobile.domain.replicated.user.profile

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.domain.replicated.security.pow.ProofOfWorkVO
import network.bisq.mobile.domain.utils.hexToByteArray

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

val UserProfileVO.id get() = networkId.pubKey.id

val UserProfileVO.pubKeyHashAsByteArray: ByteArray get() = networkId.pubKey.hash.hexToByteArray()
