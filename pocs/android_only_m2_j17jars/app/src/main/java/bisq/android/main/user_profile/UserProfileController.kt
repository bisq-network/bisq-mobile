package bisq.android.main.user_profile

import bisq.security.DigestUtil
import bisq.security.SecurityService
import bisq.user.UserService
import bisq.user.identity.NymIdGenerator
import bisq.user.identity.UserIdentity
import bisq.user.identity.UserIdentityService
import lombok.extern.slf4j.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Random
import java.util.concurrent.CompletableFuture

@Slf4j
class UserProfileController(
    userService: UserService,
    private val securityService: SecurityService
) {
    companion object {
        private const val AVATAR_VERSION = 0
        val log: Logger = LoggerFactory.getLogger(UserProfileController::class.java)
    }
    private val view: UserProfileView
    private val model = UserProfileModel()
    private val userIdentityService: UserIdentityService = userService.userIdentityService

    init {
        view = UserProfileView(this, model)
    }

    fun initialize() {
        val userIdentities = userIdentityService.userIdentities
        if (userIdentities.isEmpty()) {
            // Generate
            onGenerateKeyPair()
        } else {
            // If we have already a user profile we don't do anything. Leave it to the parent
            // controller to skip and not even create initialize controller.
            UserProfileController.log.warn("We have already a user profile.")
        }
    }

    fun createUserProfile(): CompletableFuture<UserIdentity> {
        // Mock UI action
        return createUserProfile("Android user " + Random().nextInt(100))
    }

    // Called from UI event (e.g. button click at re-generate button)
    private fun onGenerateKeyPair() {
        val keyPair = securityService.keyBundleService.generateKeyPair()
        model.keyPair = keyPair
        val pubKeyHash = DigestUtil.hash(keyPair.public.encoded)
        model.pubKeyHash = pubKeyHash
        val proofOfWork = userIdentityService.mintNymProofOfWork(pubKeyHash)
        model.proofOfWork = proofOfWork
        val powSolution = proofOfWork.solution
        val nym = NymIdGenerator.generate(pubKeyHash, powSolution)
        model.nym.set(nym) // nym will be created on demand from pubKeyHash and pow
        // CatHash is in desktop, needs to be reimplemented or the javafx part extracted and refactored into a non javafx lib
        //  Image image = CatHash.getImage(pubKeyHash,
        //                                powSolution,
        //                                CURRENT_AVATARS_VERSION,
        //                                CreateProfileModel.CAT_HASH_IMAGE_SIZE);
    }

    // Called from UI event (e.g. button click create profile button and passing the string from the nickname text input)
    private fun onCreateUserProfile(nickName: String) {
        createUserProfile(nickName)
    }

    private fun createUserProfile(nickName: String): CompletableFuture<UserIdentity> {
        // UI can listen to that state change and show busy animation
        model.isBusy.set(true)
        return userIdentityService.createAndPublishNewUserProfile(
            nickName,
            model.keyPair,
            model.pubKeyHash,
            model.proofOfWork,
            AVATAR_VERSION,
            model.terms.get(),
            model.statement.get()
        )
            .whenComplete { userIdentity: UserIdentity?, throwable: Throwable? ->
                // UI can listen to that state change and stop busy animation and show close button
                model.isBusy.set(false)
            }
    }
}
