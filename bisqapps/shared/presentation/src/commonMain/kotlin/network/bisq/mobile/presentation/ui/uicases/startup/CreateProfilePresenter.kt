package network.bisq.mobile.presentation.ui.uicases.startup

import androidx.navigation.NavController
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

open class CreateProfilePresenter(
    mainPresenter: MainPresenter,
    private val navController: NavController,
    private val userProfileService: UserProfileServiceFacade
) : BasePresenter(mainPresenter) {

    private val log = Logger.withTag(this::class.simpleName ?: "CreateProfilePresenter")

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

    override fun onViewAttached() {
        onGenerateKeyPair()

        // Currently we just always show the create profile page.
        // We need to make the UI behaving to the intended use case.
        // 1. After loading screen -> check if there is an existing user profile by
        // calling `userProfileRepository.service.hasUserProfile()`
        // 1a. If there is an existing user profile, do not show create user profile screen,
        // but show user profile is some not yet defined way (right top corner in Desktop shows user profile).
        // `userProfileRepository.service.applySelectedUserProfile()` fills the user profile data to
        // userProfileRepository.model to be used in the UI.
        // 1b. If there is no existing user profile, show create profile screen and call
        // `onGenerateKeyPair()` when view is ready.
    }

    fun onGenerateKeyPair() {
        // takes 200 -1000 ms
        CoroutineScope(BackgroundDispatcher).launch {
            setGenerateKeyPairInProgress(true)
            log.i { "Show busy animation for generateKeyPair" }
            userProfileService.generateKeyPair { id, nym ->
                setId(id)
                setNym(nym)
                //todo show new profile image
            }
            setGenerateKeyPairInProgress(false)
            log.i { "Hide busy animation for generateKeyPair" }
        }
    }

    fun onCreateAndPublishNewUserProfile() {
        if (nickName.value.isNotEmpty()) {
            CoroutineScope(BackgroundDispatcher).launch {
                setCreateAndPublishInProgress(true)
                log.i { "Show busy animation for createAndPublishInProgress" }
                userProfileService.createAndPublishNewUserProfile(nickName.value)
                log.i { "Hide busy animation for createAndPublishInProgress" }
                setCreateAndPublishInProgress(false)

                CoroutineScope(Dispatchers.Main).launch {
                    // todo stop busy animation in UI
                    navController.navigate(Routes.TrustedNodeSetup.name) {
                        popUpTo(Routes.CreateProfile.name) { inclusive = true }
                    }
                }
            }
        }
    }
}
