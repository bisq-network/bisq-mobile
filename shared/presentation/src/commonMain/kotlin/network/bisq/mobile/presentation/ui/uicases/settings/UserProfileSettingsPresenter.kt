package network.bisq.mobile.presentation.ui.uicases.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.client.replicated_model.user.profile.UserProfile
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.utils.DateUtils

class UserProfileSettingsPresenter(
    private val userProfileServiceFacade: UserProfileServiceFacade,
    mainPresenter: MainPresenter): BasePresenter(mainPresenter), IUserProfileSettingsPresenter {

    companion object {
        const val DEFAULT_UNKNOWN_VALUE = "N/A"
    }

    private val _reputation = MutableStateFlow(DEFAULT_UNKNOWN_VALUE)
    override val reputation: StateFlow<String> = _reputation
    private val _lastUserActivity = MutableStateFlow(DEFAULT_UNKNOWN_VALUE)
    override val lastUserActivity: StateFlow<String> = _lastUserActivity
    private val _profileAge = MutableStateFlow(DEFAULT_UNKNOWN_VALUE)
    override val profileAge: StateFlow<String> = _profileAge
    private val _profileId = MutableStateFlow(DEFAULT_UNKNOWN_VALUE)
    override val profileId: StateFlow<String> = _profileId
    private val _botId = MutableStateFlow(DEFAULT_UNKNOWN_VALUE)
    override val botId: StateFlow<String> = _botId

    override fun onViewAttached() {
        super.onViewAttached()
        backgroundScope.launch {
            userProfileServiceFacade.getSelectedUserProfile()?.let { it ->
//                _reputation.value = it.reputation // TODO reputation?
//                _lastUserActivity = it.lastUserActivity // TODO implement this feature? - we will need to persist profile
                setProfileAge(it)
            }
        }
    }

    private fun setProfileAge(userProfile: UserProfile) {
        userProfile.publishDate?.let { pd ->
            _profileAge.value = DateUtils.periodFrom(pd).let {
                listOfNotNull(
                    if (it.first > 0) "${it.first} years" else null,
                    if (it.second > 0) "${it.second} months" else null,
                    if (it.third > 0) "${it.third} days" else null
                ).ifEmpty { listOf("less than a day") }.joinToString(", ")
            }
        } ?: DEFAULT_UNKNOWN_VALUE
    }

    override fun onDelete() {
        TODO("Not yet implemented")
    }

    override fun onSave() {
        TODO("Not yet implemented")
    }

}