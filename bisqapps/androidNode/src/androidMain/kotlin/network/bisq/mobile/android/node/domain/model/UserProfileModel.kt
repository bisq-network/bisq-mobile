package network.bisq.mobile.android.node.domain.model

import bisq.common.observable.Observable
import bisq.security.pow.ProofOfWork
import lombok.Getter
import lombok.Setter
import java.security.KeyPair

/**
 * TODO do we need to make this shared? If so it involves getting rid of the bisq.* deps, lombok and include java.security in shared..
 * Most probably we don't and this can be just part of androidNode (we'll know when the bisq-apis gets defined)
 */
@Getter
class UserProfileModel {
    @Setter
    var keyPair: KeyPair? = null

    @Setter
    lateinit var pubKeyHash: ByteArray

    @Setter
    var proofOfWork: ProofOfWork? = null

    private val userName = Observable<String>()
    val terms = Observable("")
    val statement = Observable("")
    val nym = Observable<String>()
    private val nickName = Observable<String>()
    private val profileId = Observable<String>()

    val isBusy = Observable<Boolean>()
}
