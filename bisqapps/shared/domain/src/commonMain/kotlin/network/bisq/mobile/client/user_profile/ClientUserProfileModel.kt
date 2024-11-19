package network.bisq.mobile.domain.client.main.user_profile

import network.bisq.mobile.client.replicated_model.security.keys.KeyPair
import network.bisq.mobile.client.replicated_model.security.pow.ProofOfWork
import network.bisq.mobile.domain.user_profile.UserProfileModel

class ClientUserProfileModel : UserProfileModel() {
    lateinit var preparedDataAsJson: String
    lateinit var keyPair: KeyPair
    lateinit var proofOfWork: ProofOfWork
}