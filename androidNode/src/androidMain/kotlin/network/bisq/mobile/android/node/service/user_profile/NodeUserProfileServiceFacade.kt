package network.bisq.mobile.android.node.service.user_profile

import bisq.common.encoding.Hex
import bisq.common.observable.Pin
import bisq.security.DigestUtil
import bisq.security.SecurityService
import bisq.security.pow.ProofOfWork
import bisq.user.UserService
import bisq.user.identity.NymIdGenerator
import bisq.user.profile.UserProfileService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.mapping.Mappings
import network.bisq.mobile.android.node.service.AndroidNodeCatHashService
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import java.security.KeyPair
import java.util.Base64
import java.util.Random
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min

/**
 * This is a facade to the Bisq Easy libraries UserIdentityService and UserProfileServices.
 * It provides the API for the users profile presenter to interact with that domain.
 * It uses in a in-memory model for the relevant data required for the presenter to reflect the domains state.
 * Persistence is done inside the Bisq Easy libraries.
 */
class NodeUserProfileServiceFacade(private val applicationService: AndroidApplicationService.Provider) :
    ServiceFacade(), UserProfileServiceFacade {

    companion object {
        private const val AVATAR_VERSION = 0

        // Image size > 60px would not get cached
        private const val DEFAULT_SIZE = 60
    }

    // Dependencies
    private val securityService: SecurityService by lazy { applicationService.securityService.get() }
    private val userService: UserService by lazy { applicationService.userService.get() }
    private val userProfileService: UserProfileService by lazy { userService.userProfileService }
    private val catHashService: AndroidNodeCatHashService by lazy { applicationService.androidCatHashService.get() }

    private val _ignoredProfileIds: MutableStateFlow<Set<String>> = MutableStateFlow(
        emptySet()
    )
    override val ignoredProfileIds: StateFlow<Set<String>> get() = _ignoredProfileIds.asStateFlow()

    // Properties
    private val _selectedUserProfile: MutableStateFlow<UserProfileVO?> = MutableStateFlow(null)
    override val selectedUserProfile: StateFlow<UserProfileVO?> get() = _selectedUserProfile.asStateFlow()

    private val _numUserProfiles = MutableStateFlow(0)
    override val numUserProfiles: StateFlow<Int> get() = _numUserProfiles.asStateFlow()

    private val avatarMap = ConcurrentHashMap<String, PlatformImage?>()

    // Misc
    private var pubKeyHash: ByteArray? = null
    private var keyPair: KeyPair? = null
    private var proofOfWork: ProofOfWork? = null
    private var numUserProfilesPin: Pin? = null

    override fun activate() {
        super<ServiceFacade>.activate()

        serviceScope.launch(Dispatchers.Default) {
            _selectedUserProfile.value = getSelectedUserProfile()
            launchIO {
                getIgnoredUserProfileIds()
            }
        }

        numUserProfilesPin = userProfileService.numUserProfiles.addObserver { num ->
            serviceScope.launch {
                val value = num ?: 0
                if (_numUserProfiles.value != value) {
                    _numUserProfiles.value = value
                }
            }
        }

    }

    override fun deactivate() {
        numUserProfilesPin?.unbind()
        numUserProfilesPin = null
        _numUserProfiles.value = 0
        avatarMap.clear()
        super<ServiceFacade>.deactivate()
    }

    // API
    override suspend fun hasUserProfile(): Boolean {
        return userService.userIdentityService.userIdentities.isNotEmpty()
    }

    override suspend fun generateKeyPair(imageSize: Int, result: (String, String, PlatformImage?) -> Unit) {
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
            pubKeyHash, solution, 0, imageSize.toDouble()
        )
        result(id!!, nym!!, profileIcon)
    }

    override suspend fun createAndPublishNewUserProfile(nickName: String) {
        userService.userIdentityService.createAndPublishNewUserProfile(
            nickName, keyPair, pubKeyHash, proofOfWork, AVATAR_VERSION, "", ""
        ).join()

        pubKeyHash = null
        keyPair = null
        proofOfWork = null

        _selectedUserProfile.value = getSelectedUserProfile()
    }

    override suspend fun updateAndPublishUserProfile(
        statement: String?, terms: String?
    ): Result<UserProfileVO> {
        try {
            val selectedUserIdentity = userService.userIdentityService.selectedUserIdentity
            userService.userIdentityService.editUserProfile(
                selectedUserIdentity, terms ?: "", statement ?: ""
            ).join()

            pubKeyHash = null
            keyPair = null
            proofOfWork = null

            val updatedProfile = getSelectedUserProfile()
            _selectedUserProfile.value = updatedProfile
            return if (updatedProfile == null) {
                Result.failure(IllegalStateException("Selected user profile is null after update"))
            } else {
                Result.success(updatedProfile)
            }
        } catch (e: Exception) {
            log.e(e) { "Failed to republish user profile: ${e.message}" }
            return Result.failure(e)
        }
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
            Mappings.UserProfileMapping.fromBisq2Model(it)
        }
    }

    override suspend fun findUserProfile(profileId: String): UserProfileVO? {
        val userProfile = userProfileService.findUserProfile(profileId)
        return if (userProfile.isPresent) {
            Mappings.UserProfileMapping.fromBisq2Model(userProfile.get())
        } else {
            null
        }
    }

    override suspend fun findUserProfiles(ids: List<String>): List<UserProfileVO> {
        return ids.mapNotNull { id -> findUserProfile(id) }
    }

    override suspend fun getUserAvatar(userProfile: UserProfileVO): PlatformImage? {
        val key = "${userProfile.id}-v${userProfile.avatarVersion}"
        avatarMap[key]?.let { return it }
        return generateCatHash(key, userProfile)
    }

    override suspend fun getUserPublishDate(): Long {
        return userService.userIdentityService.selectedUserIdentity.userProfile.publishDate
    }

    override suspend fun userActivityDetected() {
        userService.republishUserProfileService.userActivityDetected()
    }

    private suspend fun generateCatHash(
        key: String,
        userProfile: UserProfileVO
    ): PlatformImage? {
        return withContext(Dispatchers.IO) {
            avatarMap.computeIfAbsent(key) {
                try {
                    log.d { "Generating avatar for ${userProfile.nym} on background thread" }
                    catHashService.getImage(
                        Base64.getDecoder().decode(userProfile.networkId.pubKey.hash),
                        Base64.getDecoder().decode(userProfile.proofOfWork.solutionEncoded),
                        userProfile.avatarVersion,
                        DEFAULT_SIZE.toDouble()
                    )
                } catch (e: Exception) {
                    log.e(e) { "Avatar generation failed for ${userProfile.nym}" }
                    null
                }
            }
        }
    }

    // Private
    private suspend fun createSimulatedDelay(powDuration: Long) {
        // Proof of work creation for difficulty 65536 takes about 50 ms to 100 ms on a 4 GHz Intel Core i7.
        // Target duration would be 200-1000 ms, but it is hard to find the right difficulty that works
        // well also for low-end CPUs. So we take a rather safe lower difficulty value and add here some
        // delay to not have a too fast flicker-effect in the UI when recreating the nym.
        // We add a min delay of 200 ms with some randomness to make the usage of the proof of work more
        // visible.
        val random: Int = Random().nextInt(800)
        // Limit to 200-2000 ms
        delay(min(1000.0, max(200.0, (200 + random - powDuration).toDouble())).toLong())
    }

    override suspend fun ignoreUserProfile(profileId: String) {
        require(profileId.isNotBlank()) { "Profile ID cannot be blank" }
        val userProfile = userProfileService.findUserProfile(profileId)
            .orElseThrow { IllegalArgumentException("User profile not found for id: $profileId") }

        userProfileService.ignoreUserProfile(userProfile)
        getIgnoredUserProfileIds() // updates ignoredUserIds
    }

    override suspend fun undoIgnoreUserProfile(profileId: String) {
        require(profileId.isNotBlank()) { "Profile ID cannot be blank" }
        val userProfile = userProfileService.findUserProfile(profileId)
            .orElseThrow { IllegalArgumentException("User profile not found for id: $profileId") }

        userProfileService.undoIgnoreUserProfile(userProfile)
        getIgnoredUserProfileIds() // updates ignoredUserIds
    }

    override suspend fun isUserIgnored(profileId: String): Boolean {
        return userProfileService.isChatUserIgnored(profileId)
    }

    override suspend fun getIgnoredUserProfileIds(): Set<String> {
        val ids = userProfileService.ignoredUserProfileIds
        _ignoredProfileIds.value = ids.toSet() // to create a new set to trigger updates correctly
        return ids
    }
}