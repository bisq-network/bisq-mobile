package bisq.android.main.user_profile

import bisq.common.observable.Observable
import bisq.security.pow.ProofOfWork
import lombok.Getter
import lombok.Setter
import java.security.KeyPair

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
