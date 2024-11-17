package network.bisq.mobile.android.node.domain.user_profile

import bisq.common.encoding.Hex
import bisq.security.DigestUtil
import bisq.security.SecurityService
import bisq.user.UserService
import bisq.user.identity.NymIdGenerator
import bisq.user.identity.UserIdentity
import co.touchlab.kermit.Logger
import network.bisq.mobile.domain.user_profile.UserProfileFacade
import network.bisq.mobile.domain.user_profile.UserProfileModel
import java.util.Random
import kotlin.math.max
import kotlin.math.min

/**
 * This is a facade to the Bisq 2 libraries UserIdentityService and UserProfileServices.
 * It provides the API for the users profile presenter to interact with that domain.
 * It uses in a in-memory model for the relevant data required for the presenter to reflect the domains state.
 * Persistence is done inside the Bisq 2 libraries.
 */
class NodeUserProfileFacade(
    override val model: UserProfileModel,
    val securityService: SecurityService,
    val userService: UserService
) :
    UserProfileFacade {

    companion object {
        private const val AVATAR_VERSION = 0
    }

    val log = Logger.withTag("NodeUserProfileFacade")


    override fun hasUserProfile(): Boolean {
        return userService.userIdentityService.userIdentities.isEmpty()
    }

    override suspend fun generateKeyPair() {
        model as NodeUserProfileModel
        val keyPair = securityService.keyBundleService.generateKeyPair()
        model.keyPair = keyPair
        val pubKeyHash = DigestUtil.hash(keyPair.public.encoded)
        model.pubKeyHash = pubKeyHash
        model.setId(Hex.encode(pubKeyHash))
        val ts = System.currentTimeMillis()
        val proofOfWork = userService.userIdentityService.mintNymProofOfWork(pubKeyHash)
        val powDuration = System.currentTimeMillis() - ts
        log.i("Proof of work creation completed after $powDuration ms")
        createSimulatedDelay(powDuration)
        model.proofOfWork = proofOfWork
        val powSolution = proofOfWork.solution
        val nym = NymIdGenerator.generate(pubKeyHash, powSolution)
        model.setNym(nym)

        // CatHash is in desktop, needs to be reimplemented or the javafx part extracted and refactored into a non javafx lib
        //  Image image = CatHash.getImage(pubKeyHash,
        //                                powSolution,
        //                                CURRENT_AVATARS_VERSION,
        //                                CreateProfileModel.CAT_HASH_IMAGE_SIZE);
    }

    override suspend fun createAndPublishNewUserProfile() {
        model as NodeUserProfileModel
        model.setIsBusy(true)  // UI should start busy animation based on that property
        userService.userIdentityService.createAndPublishNewUserProfile(
            model.nickName.value,
            model.keyPair,
            model.pubKeyHash,
            model.proofOfWork,
            AVATAR_VERSION,
            "",
            ""
        )
            .whenComplete { userIdentity: UserIdentity?, throwable: Throwable? ->
                // UI should stop busy animation and show `next` button
                model.setIsBusy(false)
            }
    }

    override fun findUserProfile(id: String): UserProfileModel? {
        return getUserProfiles().find { model -> model.id.equals(id) }
    }

    override fun getUserProfiles(): Sequence<UserProfileModel> {
        return userService.userIdentityService.userIdentities
            .asSequence()
            .map { userIdentity ->
                val userProfile = userIdentity.userProfile
                val model = NodeUserProfileModel()
                model.setNickName(userProfile.nickName)
                model.setNym(userProfile.nym)
                model.setId(userProfile.id)
                model.keyPair = userIdentity.identity.keyBundle.keyPair
                model.pubKeyHash = userIdentity.userProfile.pubKeyHash
                model.proofOfWork = userIdentity.userProfile.proofOfWork
                model
            }
    }

    private fun createSimulatedDelay(powDuration: Long) {
        try {
            // Proof of work creation for difficulty 65536 takes about 50 ms to 100 ms on a 4 GHz Intel Core i7.
            // Target duration would be 500-2000 ms, but it is hard to find the right difficulty that works
            // well also for low-end CPUs. So we take a rather safe lower difficulty value and add here some
            // delay to not have a too fast flicker-effect in the UI when recreating the nym.
            // We add a min delay of 200 ms with some randomness to make the usage of the proof of work more
            // visible.
            val random: Int = Random().nextInt(100)
            // Limit to 200-2000 ms
            Thread.sleep(
                min(2000.0, max(200.0, (200 + random - powDuration).toDouble()))
                    .toLong()
            )
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }
    }
}