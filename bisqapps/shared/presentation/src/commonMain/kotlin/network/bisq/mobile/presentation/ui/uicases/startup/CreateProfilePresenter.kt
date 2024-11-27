package network.bisq.mobile.presentation.ui.uicases.startup

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

open class CreateProfilePresenter(
    mainPresenter: MainPresenter,
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
    private val coroutineScope =
        CoroutineScope(Dispatchers.Main) // rootNavigator.navigate requires Dispatchers.Main
    private var job: Job? = null

    // Lifecycle
    override fun onViewAttached() {
        generateKeyPair()
    }

    override fun onViewUnattaching() {
        cancelJob()
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
                setCreateAndPublishInProgress(true)
                log.i { "Show busy animation for createAndPublishInProgress" }
                userProfileService.createAndPublishNewUserProfile(nickName.value)
                log.i { "Hide busy animation for createAndPublishInProgress" }
                setCreateAndPublishInProgress(false)

                // todo stop busy animation in UI
                // Skip for now the TrustedNodeSetup until its fully implemented with persisting the api URL.
                /* rootNavigator.navigate(Routes.TrustedNodeSetup.name) {
                     popUpTo(Routes.CreateProfile.name) { inclusive = true }
                 }  */
                rootNavigator.navigate(Routes.TabContainer.name) {
                    popUpTo(Routes.CreateProfile.name) { inclusive = true }
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
            // takes 200 -1000 ms
            userProfileService.generateKeyPair { id, nym ->
                setId(id)
                setNym(nym)
                //todo show new profile image
            }
            setGenerateKeyPairInProgress(false)
            log.i { "Hide busy animation for generateKeyPair" }
        }
    }

    private fun cancelJob() {
        try {
            job?.cancel()
            job = null
        } catch (e: CancellationException) {
            log.e("Job cancel failed", e)
        }
    }
}
