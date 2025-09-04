package network.bisq.mobile.presentation.ui.uicases.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.model.User
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.data.repository.UserRepository
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.domain.service.reputation.ReputationServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.DateUtils
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter

class UserProfileSettingsPresenter(
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val reputationServiceFacade: ReputationServiceFacade,
    private val userRepository: UserRepository,
    private val connectivityService: ConnectivityService,
    mainPresenter: MainPresenter
) : BasePresenter(mainPresenter), IUserProfileSettingsPresenter {

    companion object {
        @Deprecated("Use getLocalizedNA() for localized fallback")
        const val DEFAULT_UNKNOWN_VALUE = "N/A"

        /**
         * Get localized "N/A" value
         */
        fun getLocalizedNA(): String = "data.na".i18n()
    }

    private val _uniqueAvatar = MutableStateFlow(userRepository.data.value?.uniqueAvatar)
    override val uniqueAvatar: StateFlow<PlatformImage?> get() = _uniqueAvatar.asStateFlow()

    private val _reputation = MutableStateFlow(getLocalizedNA())
    override val reputation: StateFlow<String> get() = _reputation.asStateFlow()
    private val _lastUserActivity = MutableStateFlow(getLocalizedNA())
    override val lastUserActivity: StateFlow<String> get() = _lastUserActivity.asStateFlow()
    private val _profileAge = MutableStateFlow(getLocalizedNA())
    override val profileAge: StateFlow<String> get() = _profileAge.asStateFlow()
    private val _profileId = MutableStateFlow(getLocalizedNA())
    override val profileId: StateFlow<String> get() = _profileId.asStateFlow()
    private val _nickname = MutableStateFlow(getLocalizedNA())
    override val nickname: StateFlow<String> get() = _nickname.asStateFlow()
    private val _botId = MutableStateFlow(getLocalizedNA())
    override val botId: StateFlow<String> get() = _botId.asStateFlow()
    private val _tradeTerms = MutableStateFlow("")
    override val tradeTerms: StateFlow<String> get() = _tradeTerms.asStateFlow()
    private val _statement = MutableStateFlow("")
    override val statement: StateFlow<String> get() = _statement.asStateFlow()

    private val _showLoading = MutableStateFlow(false)
    override val showLoading: StateFlow<Boolean> get() = _showLoading.asStateFlow()

    private val _showDeleteOfferConfirmation = MutableStateFlow(false)
    override val showDeleteProfileConfirmation: StateFlow<Boolean> get() = _showDeleteOfferConfirmation.asStateFlow()
    override fun setShowDeleteProfileConfirmation(value: Boolean) {
        _showDeleteOfferConfirmation.value = value
    }

    override val connectivityStatus: StateFlow<ConnectivityService.ConnectivityStatus> get() = connectivityService.status

    override fun onViewAttached() {
        super.onViewAttached()

        launchUI {
            val user = withContext(IODispatcher) { userRepository.fetch() }
            val userProfile = withContext(IODispatcher) { userProfileServiceFacade.getSelectedUserProfile() }
            userProfile?.let {
                setProfileId(it)
                setBotId(it)
                setNickname(it)
                setStatement(it)
                setTradeTerms(it)

                val profileAge = withContext(IODispatcher) { reputationServiceFacade.getProfileAge(it.id) }
                setProfileAge(profileAge.getOrNull())
            }
            val reputation = if (userProfile != null) {
                withContext(IODispatcher) { reputationServiceFacade.getReputation(userProfile.id) }
            } else null
                user?.let {
                setAvatar(it)
                setLastActivity(it)
            }
            reputation?.getOrNull()?.let {
                setReputation(it)
            }
        }
    }


    private fun setAvatar(it: User) {
        _uniqueAvatar.value = it.uniqueAvatar
    }

    private fun setReputation(reputation: ReputationScoreVO) {
        _reputation.value = reputation.totalScore.toString()
    }

    private fun setStatement(user: UserProfileVO) {
        _statement.value = user.statement
    }

    private fun setTradeTerms(user: UserProfileVO) {
        _tradeTerms.value = user.terms
    }

    private fun setLastActivity(user: User) {
        _lastUserActivity.value = user.lastActivity?.let { DateUtils.lastSeen(it) } ?: getLocalizedNA()
    }

    private fun setBotId(userProfile: UserProfileVO) {
        _botId.value = userProfile.nym
    }

    private fun setNickname(userProfile: UserProfileVO) {
        _nickname.value = userProfile.nickName
    }

    private fun setProfileId(userProfile: UserProfileVO) {
        _profileId.value = userProfile.id
    }

    private fun setProfileAge(profileAgeTimestamp: Long?) {
        if (profileAgeTimestamp != null) {
            _profileAge.value = DateUtils.formatProfileAge(profileAgeTimestamp)
        } else {
            _profileAge.value = getLocalizedNA()
        }
    }

    override fun onDelete() {
        TODO("Not yet implemented")
    }

    override fun onSave() {
        disableInteractive()
        setShowLoading(true)
        launchUI {
            try {
                val result = withContext(IODispatcher) {
                    userProfileServiceFacade.updateAndPublishUserProfile(statement.value, tradeTerms.value)
                }
                if (result.isSuccess) {
                    userRepository.updateLastActivity()?.let { setLastActivity(it) }
                    showSnackbar("mobile.settings.userProfile.saveSuccess".i18n(), isError = false)
                } else {
                    showSnackbar("mobile.settings.userProfile.saveFailure".i18n(), isError = true)
                }
            } catch (e: Exception) {
                log.e(e) { "Failed to save user profile settings" }
            } finally {
                setShowLoading(false)
                enableInteractive()
            }
        }
    }

    override fun updateTradeTerms(it: String) {
        _tradeTerms.value = it
    }

    override fun updateStatement(it: String) {
        _statement.value = it
    }

    private fun setShowLoading(show: Boolean = true) {
        launchUI {
            _showLoading.value = show
        }
    }
}