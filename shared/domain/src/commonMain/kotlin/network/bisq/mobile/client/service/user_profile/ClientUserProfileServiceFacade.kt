package network.bisq.mobile.client.service.user_profile

import io.ktor.util.decodeBase64Bytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.hexToByteArray
import okio.ByteString.Companion.decodeBase64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class ClientUserProfileServiceFacade(
    private val apiGateway: UserProfileApiGateway, private val clientCatHashService: ClientCatHashService<PlatformImage>
) : ServiceFacade(), UserProfileServiceFacade {

    private var keyMaterialResponse: KeyMaterialResponse? = null

    // Properties
    private val _selectedUserProfile: MutableStateFlow<UserProfileVO?> = MutableStateFlow(null)
    override val selectedUserProfile: StateFlow<UserProfileVO?> get() = _selectedUserProfile.asStateFlow()

    private val avatarMap: MutableMap<String, PlatformImage?> = mutableMapOf<String, PlatformImage?>()
    private val avatarMapMutex = Mutex()

    private var ignoredUserIdsCache: Set<String>? = null
    private val ignoredUserIdsMutex = Mutex()

    // Misc
    override fun activate() {
        super<ServiceFacade>.activate()

        serviceScope.launch(Dispatchers.Default) {
            _selectedUserProfile.value = getSelectedUserProfile()
        }
    }

    override fun deactivate() {
        super<ServiceFacade>.deactivate()
    }

    // API
    override suspend fun hasUserProfile(): Boolean {
        return getUserIdentityIds().isNotEmpty()
    }

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun generateKeyPair(result: (String, String, PlatformImage?) -> Unit) {
        val ts = Clock.System.now().toEpochMilliseconds()
        val apiResult = apiGateway.getKeyMaterial()
        if (apiResult.isFailure) {
            throw apiResult.exceptionOrNull()!!
        }

        val preparedData = apiResult.getOrThrow()
        createSimulatedDelay(Clock.System.now().toEpochMilliseconds() - ts)
        val pubKeyHash: ByteArray = preparedData.id.hexToByteArray()
        val solutionEncoded = preparedData.proofOfWork.solutionEncoded
        val image: PlatformImage? = clientCatHashService.getImage(
            pubKeyHash, solutionEncoded.decodeBase64Bytes(), 0, 120
        )

        result(preparedData.id, preparedData.nym, image)
        this.keyMaterialResponse = preparedData
    }

    override suspend fun createAndPublishNewUserProfile(nickName: String) {
        if (keyMaterialResponse == null) {
            return
        }
        val apiResult = apiGateway.createAndPublishNewUserProfile(nickName, keyMaterialResponse!!)
        if (apiResult.isFailure) {
            throw apiResult.exceptionOrNull()!!
        }

        val response: CreateUserIdentityResponse = apiResult.getOrThrow()
        this.keyMaterialResponse = null
        log.i { "Call to createAndPublishNewUserProfile successful. userProfileId = ${response.userProfile.id}" }

        _selectedUserProfile.value = response.userProfile
    }

    override suspend fun updateAndPublishUserProfile(
        statement: String?, terms: String?
    ): Result<UserProfileVO> {
        try {
            // trigger exception if no selected user profile
            getSelectedUserProfile()

            val apiResult = apiGateway.updateUserProfile(statement ?: "", terms ?: "")
            if (apiResult.isFailure) {
                throw apiResult.exceptionOrNull()!!
            }

            val response: CreateUserIdentityResponse = apiResult.getOrThrow()
            this.keyMaterialResponse = null
            log.i {
                "Call to updateAndPublishUserProfile successful. new statement = ${response.userProfile.statement}, " + "new terms = ${response.userProfile.terms}"
            }

            _selectedUserProfile.value = response.userProfile
            return Result.success(response.userProfile)
        } catch (e: Exception) {
            log.e(e) { "Failed to update and publish user profile: ${e.message}" }
            return Result.failure(e)
        }
    }

    override suspend fun getUserIdentityIds(): List<String> {
        val apiResult = apiGateway.getUserIdentityIds()
        if (apiResult.isFailure) {
            throw apiResult.exceptionOrNull()!!
        }

        return apiResult.getOrThrow()
    }

    override suspend fun applySelectedUserProfile(): Triple<String?, String?, String?> {
        val userProfile = getSelectedUserProfile()
        return Triple(userProfile.nickName, userProfile.nym, userProfile.id)
    }

    override suspend fun getSelectedUserProfile(): UserProfileVO {
        try {
            val apiResult = apiGateway.getSelectedUserProfile()
            if (apiResult.isFailure) {
                throw apiResult.exceptionOrNull()!!
            }
            return apiResult.getOrThrow()
        } catch (e: Exception) {
            log.e(e) { "Failed to get selected user profile from service facade" }
            throw e
        }

    }

    override suspend fun findUserProfile(profileId: String): UserProfileVO? {
        val apiResult = apiGateway.findUserProfiles(listOf(profileId))
        val response = apiResult.getOrThrow()
        return response.firstOrNull()
    }

    override suspend fun findUserProfiles(ids: List<String>): List<UserProfileVO> {
        val apiResult = apiGateway.findUserProfiles(ids)
        return apiResult.getOrThrow()
    }

    // Private
    private suspend fun createSimulatedDelay(requestDuration: Long) {
        // Proof of work creation for difficulty 65536 takes about 50 ms to 100 ms on a 4 GHz Intel Core i7.
        // The API request is likely also quite fast
        // We design a delay of 200 - 1000 ms taking into account a random value and the requestDuration.
        // The delay should avoid a too fast flicker-effect in the UI when recreating the nym,
        // and should make the usage of the proof of work more visible.
        val random: Int = Random.nextInt(800)
        val delayDuration = min(1000.0, max(200.0, (200 + random - requestDuration).toDouble())).toLong()
        delay(delayDuration)
    }

    override suspend fun getUserAvatar(userProfile: UserProfileVO): PlatformImage? {
        return avatarMapMutex.withLock {
            if (avatarMap[userProfile.nym] == null) {
                val avatar = try {
                    val pubKeyHash = userProfile.networkId.pubKey.hash.decodeBase64()!!.toByteArray()
                    val powSolution = userProfile.proofOfWork.solutionEncoded.decodeBase64()!!.toByteArray()
                    clientCatHashService.getImage(pubKeyHash, powSolution, userProfile.avatarVersion, 120)
                } catch (e: Exception) {
                    log.e(e) { "Avatar generation failed for ${userProfile.nym}" }
                    null
                }
                avatarMap[userProfile.nym] = avatar
            }
            return avatarMap[userProfile.nym]
        }
    }

    override suspend fun ignoreUserProfile(profileId: String) {
        try {
            apiGateway.ignoreUser(profileId).getOrThrow()
            ignoredUserIdsMutex.withLock {
                ignoredUserIdsCache = (ignoredUserIdsCache ?: emptySet()) + profileId
            }
        } catch (e: Exception) {
            log.e(e) { "Failed to ignore user id: $profileId" }
            throw e
        }
    }

    override suspend fun undoIgnoreUserProfile(profileId: String) {
        try {
            apiGateway.undoIgnoreUser(profileId).getOrThrow()
            ignoredUserIdsMutex.withLock {
                ignoredUserIdsCache = (ignoredUserIdsCache ?: emptySet()) - profileId
            }
        } catch (e: Exception) {
            log.e(e) { "Failed to undo ignore user id: $profileId" }
            throw e
        }
    }

    override suspend fun isUserIgnored(profileId: String): Boolean {
        val cached = ignoredUserIdsMutex.withLock { ignoredUserIdsCache }
        if (cached != null) return profileId in cached

        val fresh = apiGateway.getIgnoredUserIds().getOrThrow().toSet()
        return ignoredUserIdsMutex.withLock {
            ignoredUserIdsCache = fresh
            profileId in fresh
        }
    }

    override suspend fun getIgnoredUserProfileIds(): List<String> {
        val cached = ignoredUserIdsMutex.withLock { ignoredUserIdsCache }
        if (cached != null) return cached.toList()
        try {
            val fetched: Set<String> = apiGateway.getIgnoredUserIds().getOrThrow().toSet()
            return ignoredUserIdsMutex.withLock {
                ignoredUserIdsCache = fetched
                fetched.toList()
            }
        } catch (e: Exception) {
            log.e(e) { "Failed to fetch ignored user IDs" }
            throw e
        }

    }
}