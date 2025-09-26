package network.bisq.mobile.presentation.ui.uicases.startup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.model.User
import network.bisq.mobile.domain.data.repository.UserRepository
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.error.GenericErrorHandler
import network.bisq.mobile.presentation.ui.navigation.Routes

open class CreateProfilePresenter(
    mainPresenter: MainPresenter,
    private val userRepository: UserRepository,
    private val userProfileService: UserProfileServiceFacade
) : BasePresenter(mainPresenter) {

    companion object {
        // The png files used for the image composition are 300 px, thus this is the max size.
        // The view use 140.dp, which translates with a screen density factor of 2 to 280 px
        // Most modern phones have rather > 2.5, but size > 300 will not provide better image quality.
        // The CatHashService also limits to 300px.
        const val IMAGE_SIZE_IN_PX = 300
    }

    // Properties
    private val _id = MutableStateFlow("")
    val id: StateFlow<String> get() = _id.asStateFlow()
    private fun setId(value: String) {
        _id.value = value
    }

    private val _nym = MutableStateFlow("")
    val nym: StateFlow<String> get() = _nym.asStateFlow()
    private fun setNym(value: String) {
        _nym.value = value
    }

    private val _profileIcon = MutableStateFlow<PlatformImage?>(null)
    val profileIcon: StateFlow<PlatformImage?> get() = _profileIcon.asStateFlow()
    private fun setProfileIcon(value: PlatformImage?) {
        _profileIcon.value = value
    }

    private val _nickName = MutableStateFlow("")
    val nickName: StateFlow<String> get() = _nickName.asStateFlow()
    fun setNickname(value: String) {
        // Trim leading/trailing whitespace to avoid accidental spaces causing validation or submission errors
        _nickName.value = value.trim()
    }

    private val _nickNameValid = MutableStateFlow(false)
    val nickNameValid: StateFlow<Boolean> get() = _nickNameValid.asStateFlow()

    private val _generateKeyPairInProgress = MutableStateFlow(false)
    val generateKeyPairInProgress: StateFlow<Boolean> get() = _generateKeyPairInProgress.asStateFlow()

    private val _createAndPublishInProgress = MutableStateFlow(false)
    val createAndPublishInProgress: StateFlow<Boolean> get() = _createAndPublishInProgress.asStateFlow()

    // Misc
    private var job: Job? = null

    // Lifecycle
    override fun onViewAttached() {
        super.onViewAttached()
        generateKeyPair()
    }

    override fun onViewUnattaching() {
        cancelJob()
        super.onViewUnattaching()
    }

    init {
        // if this presenter gets to work, it means there is no profile saved
    }

    fun validateNickname(nickname: String): String? {
        val trimmed = nickname.trim()
        return when {
            trimmed.isEmpty() -> "mobile.createProfile.nickname.minLength".i18n()
            trimmed.length > 100 -> "mobile.createProfile.nickname.maxLength".i18n()
            else -> null
        }.also {
            _nickNameValid.value = it == null
        }
    }

    // UI handlers
    fun onGenerateKeyPair() {
        generateKeyPair()
    }

    fun onCreateAndPublishNewUserProfile() {
        val toSubmit = nickName.value.trim()
        if (toSubmit.isNotEmpty()) {
            // We would never call generateKeyPair while generateKeyPair is not
            // completed, thus we can assign to same job reference
            job = launchIO {
                withContext(Dispatchers.Main) {
                    disableInteractive()
                    _createAndPublishInProgress.value = true
                }
                log.i { "Show busy animation for createAndPublishInProgress" }
                runCatching {
                    userProfileService.createAndPublishNewUserProfile(toSubmit)
                    // Navigate to TabContainer and completely clear the back stack
                    // This ensures the user can never navigate back to onboarding screens
                    navigateTo(Routes.TabContainer) {
                        it.popUpTo(Routes.Splash.name) { inclusive = true }
                    }

                    log.i { "Hide busy animation for createAndPublishInProgress" }
                    withContext(Dispatchers.Main) {
                        _createAndPublishInProgress.value = false
                    }
                    enableInteractive()
                }.onFailure { e ->
                    GenericErrorHandler.handleGenericError(
                        "Creating and publishing new user profile failed.",
                        e
                    )
                    _createAndPublishInProgress.value = false
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
        _generateKeyPairInProgress.value = true
        log.i { "Show busy animation for generateKeyPair" }

        runCatching {
            job = launchUI {
                withContext(Dispatchers.Default) {
                    // takes 200 -1000 ms
                    runCatching {
                        userProfileService.generateKeyPair(
                            IMAGE_SIZE_IN_PX
                        ) { id, nym, profileIcon ->
                            setId(id)
                            setNym(nym)
                            setProfileIcon(profileIcon)
                        }
                    }.onFailure {
                        disableInteractive()
                        showSnackbar("mobile.profile.generatingKeyPairFailed".i18n())
                    }
                }

                withContext(IODispatcher) {
                    userRepository.update(
                        User().copy(
                            uniqueAvatar = profileIcon.value,
                        )
                    )
                }

                _generateKeyPairInProgress.value = false
                log.i { "Hide busy animation for generateKeyPair" }
            }
        }.onFailure { e ->
            GenericErrorHandler.handleGenericError("Generating the key pair failed.", e)
            _generateKeyPairInProgress.value = false
        }
    }

    private fun cancelJob() {
        job?.cancel()
        job = null
    }
}
