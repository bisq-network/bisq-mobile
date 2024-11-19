package network.bisq.mobile.presentation.ui.uicases.startup

import androidx.navigation.NavController
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.data.repository.UserProfileRepository
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

open class CreateProfilePresenter(
    mainPresenter: MainPresenter,
    private val navController: NavController,
    private val userProfileRepository: UserProfileRepository
) : BasePresenter(mainPresenter) {

    private val log = Logger.withTag("CreateProfilePresenter")
    private val userProfileModel = userProfileRepository.model

    val nickName: StateFlow<String> = userProfileModel.nickName
    val nym: StateFlow<String> = userProfileModel.nym
    val id: StateFlow<String> = userProfileModel.id


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

    fun onNickNameChanged(value: String) {
        userProfileRepository.model.setNickName(value)
    }

    fun onGenerateKeyPair() {
        // takes 200 -1000 ms
        // todo start busy animation in UI
        CoroutineScope(BackgroundDispatcher).launch {
            userProfileRepository.model.generateKeyPairInProgress.collect { inProgress ->
                if (!inProgress) {
                    // todo stop busy animation in UI
                }
            }
        }

        CoroutineScope(BackgroundDispatcher).launch {
            userProfileRepository.service.generateKeyPair()
        }
    }

    fun onCreateAndPublishNewUserProfile() {
        // todo start busy animation in UI
        // We cannot use BackgroundDispatcher here as we get error:
        // `Method setCurrentState must be called on the main thread`
        CoroutineScope(Dispatchers.Main).launch {
            userProfileRepository.model.createAndPublishInProgress.collect { inProgress ->
                if (!inProgress) {
                    // todo stop busy animation in UI
                    navController.navigate(Routes.TrustedNodeSetup.name) {
                        popUpTo(Routes.CreateProfile.name) { inclusive = true }
                    }
                }
            }
        }

        CoroutineScope(BackgroundDispatcher).launch {
            userProfileRepository.service.createAndPublishNewUserProfile()
        }
    }
}
