package bisq.android.main.user_profile;

import java.security.KeyPair;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import bisq.common.observable.collection.ObservableSet;
import bisq.security.DigestUtil;
import bisq.security.SecurityService;
import bisq.security.pow.ProofOfWork;
import bisq.user.UserService;
import bisq.user.identity.NymIdGenerator;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserProfileController {
    private static final int AVATAR_VERSION = 0;

    private final UserProfileView view;
    private final UserProfileModel model;
    private final SecurityService securityService;
    private final UserIdentityService userIdentityService;

    public UserProfileController(UserService userService, SecurityService securityService) {
        userIdentityService = userService.getUserIdentityService();
        this.securityService = securityService;
        model = new UserProfileModel();
        view = new UserProfileView(this, model);
    }

    public void initialize() {
        ObservableSet<UserIdentity> userIdentities = userIdentityService.getUserIdentities();
        if (userIdentities.isEmpty()) {
            // Generate
            onGenerateKeyPair();
        } else {
            // If we have already a user profile we don't do anything. Leave it to the parent
            // controller to skip and not even create initialize controller.
            log.warn("We have already a user profile.");
        }
    }

    public CompletableFuture<UserIdentity> createUserProfile() {
        // Mock UI action
        return createUserProfile("Android user " + new Random().nextInt(100));
    }

    // Called from UI event (e.g. button click at re-generate button)
    private void onGenerateKeyPair() {
        KeyPair keyPair = securityService.getKeyBundleService().generateKeyPair();
        model.setKeyPair(keyPair);
        byte[] pubKeyHash = DigestUtil.hash(keyPair.getPublic().getEncoded());
        model.setPubKeyHash(pubKeyHash);
        ProofOfWork proofOfWork = userIdentityService.mintNymProofOfWork(pubKeyHash);
        model.setProofOfWork(proofOfWork);
        byte[] powSolution = proofOfWork.getSolution();
        String nym = NymIdGenerator.generate(pubKeyHash, powSolution);
        model.getNym().set(nym); // nym will be created on demand from pubKeyHash and pow
        // CatHash is in desktop, needs to be reimplemented or the javafx part extracted and refactored into a non javafx lib
        //  Image image = CatHash.getImage(pubKeyHash,
        //                                powSolution,
        //                                CURRENT_AVATARS_VERSION,
        //                                CreateProfileModel.CAT_HASH_IMAGE_SIZE);
    }

    // Called from UI event (e.g. button click create profile button and passing the string from the nickname text input)
    private void onCreateUserProfile(String nickName) {
        createUserProfile(nickName);
    }

    private CompletableFuture<UserIdentity> createUserProfile(String nickName) {
        // UI can listen to that state change and show busy animation
        model.getIsBusy().set(true);
        return userIdentityService.createAndPublishNewUserProfile(nickName,
                        model.getKeyPair(),
                        model.getPubKeyHash(),
                        model.getProofOfWork(),
                        AVATAR_VERSION,
                        model.getTerms().get(),
                        model.getStatement().get())
                .whenComplete((userIdentity, throwable) -> {
                    // UI can listen to that state change and stop busy animation and show close button
                    model.getIsBusy().set(false);
                });
    }
}
