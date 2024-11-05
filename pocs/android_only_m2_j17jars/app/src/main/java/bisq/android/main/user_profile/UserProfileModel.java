package bisq.android.main.user_profile;

import java.security.KeyPair;

import bisq.common.data.ByteArray;
import bisq.common.observable.Observable;
import bisq.security.pow.ProofOfWork;
import bisq.user.identity.UserIdentity;
import lombok.Getter;
import lombok.Setter;

@Getter
public class UserProfileModel {
    @Setter
    private KeyPair keyPair;
    @Setter
    private byte[] pubKeyHash;
    @Setter
    private ProofOfWork proofOfWork;

    private final Observable<String> userName = new Observable<>();
    private final Observable<String> terms = new Observable<>("");
    private final Observable<String> statement = new Observable<>("");
    private final Observable<String> nym = new Observable<>();
    private final Observable<String> nickName = new Observable<>();
    private final Observable<String> profileId = new Observable<>();

    private final Observable<Boolean> isBusy = new Observable<>();
}
