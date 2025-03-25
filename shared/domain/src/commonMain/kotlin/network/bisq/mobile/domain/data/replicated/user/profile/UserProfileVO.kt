package network.bisq.mobile.domain.data.replicated.user.profile

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.data.replicated.common.network.AddressByTransportTypeMapVO
import network.bisq.mobile.domain.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.domain.data.replicated.security.keys.PubKeyVO
import network.bisq.mobile.domain.data.replicated.security.keys.PublicKeyVO
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

val userProfileDemoObj = UserProfileVO(
    version = 1,
    nickName = "demo",
    proofOfWork = ProofOfWorkVO(
        payloadEncoded = "payload",
        counter = 1L,
        challengeEncoded = "challenge",
        difficulty = 2.0,
        solutionEncoded = "sol",
        duration = 100L
    ),
    avatarVersion = 1,
    networkId = NetworkIdVO(
        addressByTransportTypeMap = AddressByTransportTypeMapVO(mapOf()),
        pubKey = PubKeyVO(
            publicKey = PublicKeyVO("pub"),
            keyId = "key",
            hash = "hash",
            id = "id"
        )
    ),
    terms = "my terms",
    statement = "my statement",
    applicationVersion = "2.1.7",
    nym = "mynym",
    userName = "demo",
    publishDate = 10342435345324L
)