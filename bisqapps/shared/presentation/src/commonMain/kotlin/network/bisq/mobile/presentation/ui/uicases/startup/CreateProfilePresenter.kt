package network.bisq.mobile.presentation.ui.uicases.startup

import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.data.repository.UserProfileRepository
import network.bisq.mobile.domain.data.model.UserProfile
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.MainPresenter

open class CreateProfilePresenter(
    mainPresenter: MainPresenter,
    private val navController: NavController,
    private val userProfileRepository: UserProfileRepository
) : BasePresenter(mainPresenter), ICreateProfilePresenter {

    private val _profileName = MutableStateFlow("")
    override val profileName: StateFlow<String> = _profileName
    override val nym: StateFlow<String> = MutableStateFlow("")
    override val id: StateFlow<String> = MutableStateFlow("")

    // TODO: Not working
    init {
        CoroutineScope(BackgroundDispatcher).launch {
            userProfileRepository.data.collectLatest { userProfile ->
                userProfile?.let {
                    _profileName.value = it.name
                }
            }
        }
    }

    override fun onProfileNameChanged(newName: String) {
        _profileName.value = newName
    }

    override fun navigateToNextScreen() {
        if (_profileName.value.isNotEmpty()) {
            saveUserProfile()
            navController.navigate(Routes.TrustedNodeSetup.name) {
                popUpTo(Routes.CreateProfile.name) { inclusive = true }
            }
        }
    }

    override fun onGenerateKeyPair() {

    }

    override fun onCreateAndPublishNewUserProfile() {
        this.navigateToNextScreen()
    }

    fun saveUserProfile() {
        CoroutineScope(BackgroundDispatcher).launch {
            val updatedProfile = UserProfile().apply {
                name = _profileName.value
            }
            userProfileRepository.update(updatedProfile)
        }
    }

    /*
    fun onNicknameChanged(newNickname: String) {
        _nickname.value = newNickname
    }

    fun navigateToNextScreen() {
        if (_nickname.value.isNotEmpty()) {
            navController.navigate(Routes.TrustedNodeSetup.name) {
                popUpTo(Routes.CreateProfile.name) { inclusive = true }
            }
        }
    }
    */

}
