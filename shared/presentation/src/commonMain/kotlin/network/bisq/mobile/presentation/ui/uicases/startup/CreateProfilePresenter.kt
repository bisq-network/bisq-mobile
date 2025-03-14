package network.bisq.mobile.presentation.ui.uicases.startup

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.model.User
import network.bisq.mobile.domain.data.repository.UserRepository
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

open class CreateProfilePresenter(
    mainPresenter: MainPresenter,
    private val userRepository: UserRepository,
    private val userProfileService: UserProfileServiceFacade
) : BasePresenter(mainPresenter) {

    // Properties
    private val _id = MutableStateFlow("")
    val id: StateFlow<String> get() = _id
    private fun setId(value: String) {
        _id.value = value
    }

    private val _nym = MutableStateFlow("")
    val nym: StateFlow<String> get() = _nym
    private fun setNym(value: String) {
        _nym.value = value
    }

    private val _profileIcon = MutableStateFlow<PlatformImage?>(null)
    val profileIcon: StateFlow<PlatformImage?> get() = _profileIcon
    private fun setProfileIcon(value: PlatformImage?) {
        _profileIcon.value = value
    }

    private val _nickName = MutableStateFlow("")
    val nickName: StateFlow<String> get() = _nickName
    fun setNickname(value: String) {
        _nickName.value = value
    }

    private val _generateKeyPairInProgress = MutableStateFlow(false)
    val generateKeyPairInProgress: StateFlow<Boolean> get() = _generateKeyPairInProgress
    private fun setGenerateKeyPairInProgress(value: Boolean) {
        _generateKeyPairInProgress.value = value
    }

    private val _createAndPublishInProgress = MutableStateFlow(false)
    val createAndPublishInProgress: StateFlow<Boolean> get() = _createAndPublishInProgress
    private fun setCreateAndPublishInProgress(value: Boolean) {
        _createAndPublishInProgress.value = value
    }

    // Misc
    private val coroutineScope = CoroutineScope(Dispatchers.Main) // rootNavigator.navigate requires Dispatchers.Main
    private var job: Job? = null

    // Lifecycle
    override fun onViewAttached() {
        generateKeyPair()
    }

    override fun onViewUnattaching() {
        cancelJob()
    }

    init {
        // if this presenter gets to work, it means there is no profile saved
        backgroundScope.launch {
            userRepository.create(User())
        }
    }

    fun validateNickname(nickname: String): String? {
        return when {
            nickname.length < 3 -> "Min length: 3 characters"
            nickname.length > 256 -> "Max length: 256 characters"
            else -> null
        }
    }

    // UI handlers
    fun onGenerateKeyPair() {
        generateKeyPair()
    }

    fun onCreateAndPublishNewUserProfile() {
        if (nickName.value.isNotEmpty()) {
            // We would never call generateKeyPair while generateKeyPair is not
            // completed, thus we can assign to same job reference
            job = coroutineScope.launch {
                enableInteractive(false)
                log.i { "Show busy animation for createAndPublishInProgress" }
                setCreateAndPublishInProgress(true)
                runCatching {
                    userProfileService.createAndPublishNewUserProfile(nickName.value)

                    log.i { "Hide busy animation for createAndPublishInProgress" }
                    setCreateAndPublishInProgress(false)

                    // Skip for now the TrustedNodeSetup until its fully implemented with persisting the api URL.
                    /* navigateTo(Routes.TrustedNodeSetup) {
                         it.popUpTo(Routes.CreateProfile.name) { inclusive = true }
                     }  */
                    navigateTo(Routes.TabContainer) {
                        it.popUpTo(Routes.Onboarding.name) { inclusive = true }
                    }
                    enableInteractive()
                }.onFailure { e ->
                    // TODO give user feedback (we could have a general error screen covering usual
                    //  issues like connection issues and potential solutions)
                    // Depending on error type,
                    //  * show either Snackbar (for excepected issues) or
                    //  * ReportBugPanel for unknown critical issues
                    // define Exception handling framework, to take care of this.
                    MainPresenter._genericErrorMessage.value = "onCreateAndPublishNewUserProfile failed" + e
                    log.e("onCreateAndPublishNewUserProfile failed", e)
                    enableInteractive()
                }
            }
        }
    }

    // Private
    private fun generateKeyPair() {
        // We would never call onCreateAndPublishNewUserProfile while generateKeyPair is not
        // completed, thus we can assign to same job reference
        cancelJob()
        job = coroutineScope.launch {
            setGenerateKeyPairInProgress(true)
            log.i { "Show busy animation for generateKeyPair" }

            runCatching {
                // takes 200 -1000 ms
                userProfileService.generateKeyPair { id, nym, profileIcon ->
                    setId(id)
                    setNym(nym)
                    setProfileIcon(profileIcon)
                    backgroundScope.launch {
                        userRepository.update(User().apply {
                            uniqueAvatar = profileIcon
                            lastActivity = Clock.System.now().toEpochMilliseconds()
                        })
                    }
                }
                setGenerateKeyPairInProgress(false)
                log.i { "Hide busy animation for generateKeyPair" }
            }.onFailure { e ->
                // TODO give user feedback (we could have a general error screen covering usual
                //  issues like connection issues and potential solutions)
                MainPresenter._genericErrorMessage.value = "generateKeyPair failed: " + e
                log.e("generateKeyPair failed", e)
            }
        }
    }

    private fun cancelJob() {
        job?.cancel()
        job = null
    }
}
