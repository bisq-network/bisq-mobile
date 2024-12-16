package network.bisq.mobile.presentation.ui.uicases.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.client.replicated_model.user.profile.UserProfile
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.repository.UserRepository
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.utils.DateUtils

class UserProfileSettingsPresenter(
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val userRepository: UserRepository,
    mainPresenter: MainPresenter): BasePresenter(mainPresenter), IUserProfileSettingsPresenter {

    companion object {
        const val DEFAULT_UNKNOWN_VALUE = "N/A"
    }

    private val _uniqueAvatar = MutableStateFlow(userRepository.data.value?.uniqueAvatar)
    override val uniqueAvatar: StateFlow<PlatformImage?> get() = _uniqueAvatar

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
    private val _tradeTerms = MutableStateFlow(DEFAULT_UNKNOWN_VALUE)
    override val tradeTerms: StateFlow<String> = _tradeTerms
    private val _statement = MutableStateFlow(DEFAULT_UNKNOWN_VALUE)
    override val statement: StateFlow<String> = _statement

    override fun onViewAttached() {
        super.onViewAttached()
        backgroundScope.launch {
            userProfileServiceFacade.getSelectedUserProfile()?.let { it ->
//                _reputation.value = it.reputation // TODO reputation?
//                _lastUserActivity = it.lastUserActivity // TODO implement this feature? - we will need to persist profile
                setProfileAge(it)
                setProfileId(it)
                setBotId(it)
                // The following should be local to the app
                setLastActivity(it)
                setTradeTerms(it)
                setStatement(it)
            }
            _uniqueAvatar.value = userRepository.fetch()?.uniqueAvatar
        }
    }

    private fun setStatement(it: UserProfile) {
        // TODO define how we get this (user repository?)
        _statement.value = DEFAULT_UNKNOWN_VALUE
    }

    private fun setTradeTerms(it: UserProfile) {
        // TODO define how we get this (user repository?)
        _tradeTerms.value = DEFAULT_UNKNOWN_VALUE
    }

    private fun setLastActivity(it: UserProfile) {
        // TODO define how we get this (user repository?)
        _lastUserActivity.value = DEFAULT_UNKNOWN_VALUE
    }

    private fun setBotId(it: UserProfile) {
        _profileId.value = it.nickName ?: DEFAULT_UNKNOWN_VALUE
    }

    private fun setProfileId(it: UserProfile) {
        _profileId.value = it.id ?: DEFAULT_UNKNOWN_VALUE
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

    override fun updateTradeTerms(it: String) {
        _tradeTerms.value = it
    }

    override fun updateStatement(it: String) {
        _statement.value = it
    }

}