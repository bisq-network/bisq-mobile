package network.bisq.mobile.android.node.service.user_profile

import bisq.common.encoding.Hex
import bisq.security.DigestUtil
import bisq.security.SecurityService
import bisq.security.pow.ProofOfWork
import bisq.user.UserService
import bisq.user.identity.NymIdGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.mapping.Mappings
import network.bisq.mobile.android.node.service.AndroidNodeCatHashService
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.data.replicated.user.identity.UserIdentityVO
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.Logging
import java.security.KeyPair
import java.util.Random
import kotlin.math.max
import kotlin.math.min

/**
 * This is a facade to the Bisq 2 libraries UserIdentityService and UserProfileServices.
 * It provides the API for the users profile presenter to interact with that domain.
 * It uses in a in-memory model for the relevant data required for the presenter to reflect the domains state.
 * Persistence is done inside the Bisq 2 libraries.
 */
class NodeUserProfileServiceFacade(private val applicationService: AndroidApplicationService.Provider) :
    UserProfileServiceFacade, Logging {

    companion object {
        private const val AVATAR_VERSION = 0
    }

    // Dependencies
    private val securityService: SecurityService by lazy {
        applicationService.securityService.get()
    }
    private val userService: UserService by lazy {
        applicationService.userService.get()
    }
    private val catHashService: AndroidNodeCatHashService by lazy {
        applicationService.androidCatHashService.get()
    }

    // Properties
    private val _selectedUserProfile: MutableStateFlow<UserProfileVO?> = MutableStateFlow(null)
    override val selectedUserProfile: StateFlow<UserProfileVO?> = _selectedUserProfile


    // Misc
    private var active = false
    private val coroutineScope = CoroutineScope(BackgroundDispatcher)
    private var jobs: MutableSet<Job> = mutableSetOf()

    private var pubKeyHash: ByteArray? = null
    private var keyPair: KeyPair? = null
    private var proofOfWork: ProofOfWork? = null


    override fun activate() {
        if (active) {
            log.w { "deactivating first" }
            deactivate()
        }

        jobs += coroutineScope.launch {
            _selectedUserProfile.value = getSelectedUserProfile()
        }

        active = true
    }

    override fun deactivate() {
        if (!active) {
            log.w { "Skipping deactivation as its already deactivated" }
            return
        }

        jobs.map { it.cancel() }
        jobs.clear()

        active = false
    }

    // API
    override suspend fun hasUserProfile(): Boolean {
        return userService.userIdentityService.userIdentities.isNotEmpty()
    }

    override suspend fun generateKeyPair(result: (String, String, PlatformImage?) -> Unit) {
        keyPair = securityService.keyBundleService.generateKeyPair()
        pubKeyHash = DigestUtil.hash(keyPair!!.public.encoded)

        val ts = System.currentTimeMillis()
        proofOfWork = userService.userIdentityService.mintNymProofOfWork(pubKeyHash)
        val powDuration = System.currentTimeMillis() - ts
        log.i("Proof of work creation completed after $powDuration ms")
        createSimulatedDelay(powDuration)

        val id = Hex.encode(pubKeyHash)
        val solution = proofOfWork!!.solution
        val nym = NymIdGenerator.generate(pubKeyHash, solution)
        val profileIcon: PlatformImage = catHashService.getImage(
            pubKeyHash,
            solution,
            0,
            120.0
        )
        result(id!!, nym!!, profileIcon)
    }

    override suspend fun createAndPublishNewUserProfile(nickName: String) {
        userService.userIdentityService.createAndPublishNewUserProfile(
            nickName,
            keyPair,
            pubKeyHash,
            proofOfWork,
            AVATAR_VERSION,
            "",
            ""
        ).join()

        pubKeyHash = null
        keyPair = null
        proofOfWork = null

        _selectedUserProfile.value = getSelectedUserProfile()
    }

    override suspend fun getUserIdentityIds(): List<String> {
        return userService.userIdentityService.userIdentities.map { userIdentity -> userIdentity.id }
    }

    override suspend fun applySelectedUserProfile(): Triple<String?, String?, String?> {
        val userProfile = getSelectedUserProfile()
        return Triple(userProfile?.nickName, userProfile?.nym, userProfile?.id)
    }

    override suspend fun getSelectedUserProfile(): UserProfileVO? {
        return userService.userIdentityService.selectedUserIdentity?.userProfile?.let {
            Mappings.UserProfileMapping.fromBisq2Model(
                it
            )
        }
    }

    override suspend fun findUserIdentities(ids: List<String>): List<UserIdentityVO> {
        val idList: MutableList<UserIdentityVO> = mutableListOf()

        ids.map {
            val userIdentity = userService.userIdentityService.findUserIdentity(it)
            if (userIdentity.isPresent) {
                idList.add(Mappings.UserIdentityMapping.fromBisq2Model(userIdentity.get()))
            }
        }

        return idList

    }

    // Private
    private fun createSimulatedDelay(powDuration: Long) {
        try {
            // Proof of work creation for difficulty 65536 takes about 50 ms to 100 ms on a 4 GHz Intel Core i7.
            // Target duration would be 200-1000 ms, but it is hard to find the right difficulty that works
            // well also for low-end CPUs. So we take a rather safe lower difficulty value and add here some
            // delay to not have a too fast flicker-effect in the UI when recreating the nym.
            // We add a min delay of 200 ms with some randomness to make the usage of the proof of work more
            // visible.
            val random: Int = Random().nextInt(800)
            // Limit to 200-2000 ms
            Thread.sleep(
                min(1000.0, max(200.0, (200 + random - powDuration).toDouble()))
                    .toLong()
            )
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }
    }
}