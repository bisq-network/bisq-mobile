package network.bisq.mobile.android.node.domain.user_profile

import bisq.security.pow.ProofOfWork
import network.bisq.mobile.domain.user_profile.UserProfileModel
import java.security.KeyPair

class NodeUserProfileModel : UserProfileModel() {
    lateinit var keyPair: KeyPair
    lateinit var proofOfWork: ProofOfWork
}