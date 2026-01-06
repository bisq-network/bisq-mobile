package network.bisq.mobile.client.common.domain.service.user_profile

import io.ktor.util.decodeBase64Bytes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.websocket.ConnectionState
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventPayload
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.createEmptyImage
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.hexToByteArray
import kotlin.concurrent.Volatile
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class ClientUserProfileServiceFacade(
    private val apiGateway: UserProfileApiGateway,
    private val clientCatHashService: ClientCatHashService<PlatformImage>,
    private val json: Json,
    private val webSocketClientService: WebSocketClientService,
) : ServiceFacade(),
    UserProfileServiceFacade {
    companion object {
        private const val MIN_PAUSE_TO_NEXT_REPUBLISH = 5 * 60 * 1000L // 5 minutes in milliseconds
    }

    private var lastPublished: Long = 0L
    private val lastPublishedMutex = Mutex()

    private var keyMaterialResponse: KeyMaterialResponse? = null

    // Properties
    private val _userProfiles: MutableStateFlow<List<UserProfileVO>> = MutableStateFlow(emptyList())
    override val userProfiles = _userProfiles.asStateFlow()

    private val _selectedUserProfile: MutableStateFlow<UserProfileVO?> = MutableStateFlow(null)
    override val selectedUserProfile: StateFlow<UserProfileVO?> get() = _selectedUserProfile.asStateFlow()

    private val _numUserProfiles = MutableStateFlow(0)
    override val numUserProfiles: StateFlow<Int> get() = _numUserProfiles.asStateFlow()

    private val _ignoredProfileIds: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())
    override val ignoredProfileIds: StateFlow<Set<String>> get() = _ignoredProfileIds.asStateFlow()
    private val ignoredUserIdsMutex = Mutex()

    // Track initialization state to prevent race conditions
    @Volatile
    private var isIgnoredUsersCacheInitialized = false

    // Misc
    override suspend fun activate() {
        super<ServiceFacade>.activate()

        serviceScope.launch {
            webSocketClientService.connectionState.collectLatest { state ->
                if (state is ConnectionState.Connected) {
                    supervisorScope {
                        val selectedDeferred =
                            async {
                                try {
                                    apiGateway.getSelectedUserProfile().getOrThrow().also { _selectedUserProfile.value = it }
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    log.d("Error getting user profile in UserProfileServiceFacade: ${e.message}")
                                }
                            }

                        val profilesDeferred = refreshUserProfiles()

                        val ignoredDeferred =
                            async {
                                // Ensure ignored users cache is initialized before any hot-path calls
                                try {
                                    getIgnoredUserProfileIds()
                                    log.d { "Ignored users cache initialized successfully" }
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    log.e(e) { "Failed to initialize ignored users cache during activation" }
                                    // Set empty cache to prevent repeated network calls
                                    ignoredUserIdsMutex.withLock { _ignoredProfileIds.value = emptySet() }
                                }
                            }

                        awaitAll(selectedDeferred, profilesDeferred, ignoredDeferred)
                    }
                }
            }
        }

        observeNumUserProfiles()
    }

    override suspend fun deactivate() {
        // Clear cache state on deactivation
        serviceScope.launch {
            ignoredUserIdsMutex.withLock {
                _ignoredProfileIds.value = emptySet()
                isIgnoredUsersCacheInitialized = false
            }
        }
        super<ServiceFacade>.deactivate()
    }

    // API
    override suspend fun hasUserProfile(): Boolean = getUserIdentityIds().isNotEmpty()

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun generateKeyPair(
        imageSize: Int,
        result: (String, String, PlatformImage?) -> Unit,
    ) {
        val ts = Clock.System.now().toEpochMilliseconds()
        val apiResult = apiGateway.getKeyMaterial()
        if (apiResult.isFailure) {
            throw apiResult.exceptionOrNull()!!
        }

        val preparedData = apiResult.getOrThrow()
        createSimulatedDelay(Clock.System.now().toEpochMilliseconds() - ts)
        val pubKeyHash: ByteArray = preparedData.id.hexToByteArray()
        val solutionEncoded = preparedData.proofOfWork.solutionEncoded
        val image: PlatformImage? =
            clientCatHashService.getImage(
                pubKeyHash,
                solutionEncoded.decodeBase64Bytes(),
                0,
                imageSize,
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

        _userProfiles.update { it + response.userProfile }
        // just in case our addition was erroneous and if state changed server side
        refreshUserProfiles()
        _selectedUserProfile.value = response.userProfile
    }

    override suspend fun updateAndPublishUserProfile(
        profileId: String,
        statement: String?,
        terms: String?,
    ): Result<UserProfileVO> {
        try {
            val apiResult = apiGateway.updateUserProfile(profileId, statement ?: "", terms ?: "")
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

    override suspend fun getUserProfileIcon(userProfile: UserProfileVO): PlatformImage = getUserProfileIcon(userProfile, ClientCatHashService.DEFAULT_SIZE)

    override suspend fun getUserProfileIcon(
        userProfile: UserProfileVO,
        size: Number,
    ): PlatformImage =
        try {
            // In case we create the image we want to run it in IO context.
            // We cache the images in the catHashService if its <=120 px
            withContext(Dispatchers.IO) {
                val ts = Clock.System.now().toEpochMilliseconds()
                clientCatHashService.getImage(userProfile, size.toInt()).also {
                    log.d {
                        "Get userProfileIcon for ${userProfile.userName} took ${
                            Clock.System.now().toEpochMilliseconds() - ts
                        } ms. User profile ID=${userProfile.id}"
                    }
                }
            }
        } catch (e: Exception) {
            log.e(e) { "Failed to get user profile icon; returning fallback" }
            fallbackProfileImage()
        }

    @OptIn(ExperimentalEncodingApi::class)
    private fun fallbackProfileImage(): PlatformImage =
        try {
            // Try to decode a 1x1 transparent PNG
            val base64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Y0iYy0AAAAASUVORK5CYII="
            val bytes = Base64.decode(base64)
            PlatformImage.deserialize(bytes)
        } catch (e: Exception) {
            log.e(e) { "Failed to decode fallback PNG; using platform empty image" }
            // If PNG decode fails, platform-specific deserialize will throw
            // and we'll create an empty image in the platform implementation
            createEmptyImage()
        }

    override suspend fun getUserPublishDate(): Long = selectedUserProfile.value?.publishDate ?: 0L

    override suspend fun userActivityDetected() {
        lastPublishedMutex.withLock {
            val now = Clock.System.now().toEpochMilliseconds()
            if (now - lastPublished < MIN_PAUSE_TO_NEXT_REPUBLISH) {
                log.d { "Ignoring user activity detection due to recent activity" }
                return@withLock
            }
            apiGateway
                .triggerUserActivityDetection()
                .onSuccess { lastPublished = now }
                .onFailure { e -> log.d { "Failed to trigger user activity detection: ${e.message}" } }
        }
    }

    override suspend fun ignoreUserProfile(profileId: String) {
        try {
            apiGateway.ignoreUser(profileId).getOrThrow()
            ignoredUserIdsMutex.withLock {
                _ignoredProfileIds.value = _ignoredProfileIds.value + profileId
                isIgnoredUsersCacheInitialized = true
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
                _ignoredProfileIds.value = _ignoredProfileIds.value - profileId
                isIgnoredUsersCacheInitialized = true
            }
        } catch (e: Exception) {
            log.e(e) { "Failed to undo ignore user id: $profileId" }
            throw e
        }
    }

    override suspend fun isUserIgnored(profileId: String): Boolean = profileId in getIgnoredUserProfileIds()

    /**
     * Fast, non-suspending check for ignored users using only the in-memory cache.
     * This method is safe to call from hot paths like offer filtering.
     *
     * @param profileId The user profile ID to check
     * @return true if user is ignored (based on cache), false if not ignored or cache not initialized
     */
    fun isUserIgnoredCached(profileId: String): Boolean {
        // Fast path: check if cache is initialized and contains the user
        val cache = _ignoredProfileIds.value
        return if (isIgnoredUsersCacheInitialized) {
            profileId in cache
        } else {
            // Cache not initialized yet, assume not ignored to avoid blocking
            // The cache will be initialized during activate() and subsequent calls will be accurate
            log.v { "isUserIgnoredCached called before cache initialization for $profileId, returning false" }
            false
        }
    }

    override suspend fun getIgnoredUserProfileIds(): Set<String> {
        if (isIgnoredUsersCacheInitialized) return _ignoredProfileIds.value
        try {
            val fetched = apiGateway.getIgnoredUserIds().getOrThrow().toSet()
            val result =
                ignoredUserIdsMutex.withLock {
                    if (isIgnoredUsersCacheInitialized) {
                        // Another path (ignore/undo) initialized the cache meanwhile; keep current cache
                        _ignoredProfileIds.value
                    } else {
                        _ignoredProfileIds.value = fetched
                        isIgnoredUsersCacheInitialized = true
                        fetched
                    }
                }
            return result
        } catch (e: Exception) {
            log.e(e) { "Failed to fetch ignored user IDs" }
            throw e
        }
    }

    override suspend fun reportUserProfile(
        accusedUserProfile: UserProfileVO,
        message: String,
    ): Result<Unit> {
        val trimmedMessage = message.trim()
        if (trimmedMessage.isBlank()) {
            return Result.failure(IllegalArgumentException("Report message cannot be blank"))
        }
        return apiGateway.reportUserProfile(accusedUserProfile.networkId.pubKey.id, trimmedMessage)
    }

    override suspend fun getOwnedUserProfiles(): Result<List<UserProfileVO>> = apiGateway.getOwnedUserProfiles()

    override suspend fun selectUserProfile(id: String): Result<UserProfileVO> =
        apiGateway
            .selectUserProfile(id)
            .also { it.onSuccess { profile -> _selectedUserProfile.value = profile } }

    override suspend fun deleteUserProfile(id: String): Result<UserProfileVO> =
        apiGateway
            .deleteUserProfile(id)
            .also { result ->
                result.onSuccess { profile ->
                    _userProfiles.update { list -> list.filterNot { it.id == id } }
                    // trigger refresh just in case
                    refreshUserProfiles()
                    _selectedUserProfile.value = profile
                }
            }

    private fun observeNumUserProfiles() {
        serviceScope.launch {
            try {
                val observer = apiGateway.subscribeNumUserProfiles()
                observer.webSocketEvent.collect { webSocketEvent ->
                    if (webSocketEvent?.deferredPayload == null) {
                        return@collect
                    }

                    val webSocketEventPayload: WebSocketEventPayload<Int> =
                        WebSocketEventPayload.from(json, webSocketEvent)
                    _numUserProfiles.value = webSocketEventPayload.payload
                }
            } catch (e: Exception) {
                log.e(e) { "Failed to subscribe to numUserProfiles" }
            }
        }
    }

    private fun refreshUserProfiles(): Deferred<Unit> =
        serviceScope.async {
            try {
                getOwnedUserProfiles().onSuccess { _userProfiles.value = it }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Expected at first run
                log.d("Error getting user profiles: ${e.message}")
            }
        }
}
