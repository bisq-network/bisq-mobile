package network.bisq.mobile.presentation.ui.uicases.settings

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter

class UserProfileSettingsPresenter(
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val reputationServiceFacade: ReputationServiceFacade,
    private val userRepository: UserRepository,
    connectivityService: ConnectivityService,
    mainPresenter: MainPresenter
) : BasePresenter(mainPresenter), IUserProfileSettingsPresenter {

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
    private val _nickname = MutableStateFlow(DEFAULT_UNKNOWN_VALUE)
    override val nickname: StateFlow<String> = _nickname
    private val _botId = MutableStateFlow(DEFAULT_UNKNOWN_VALUE)
    override val botId: StateFlow<String> = _botId
    private val _tradeTerms = MutableStateFlow("")
    override val tradeTerms: StateFlow<String> = _tradeTerms
    private val _statement = MutableStateFlow("")
    override val statement: StateFlow<String> = _statement

    private val _showLoading = MutableStateFlow(false)
    override val showLoading: StateFlow<Boolean> = _showLoading

    private val _showDeleteOfferConfirmation = MutableStateFlow(false)
    override val showDeleteProfileConfirmation: StateFlow<Boolean> get() = _showDeleteOfferConfirmation
    override fun setShowDeleteProfileConfirmation(value: Boolean) {
        _showDeleteOfferConfirmation.value = value
    }

    override val connectivityStatus: StateFlow<ConnectivityService.ConnectivityStatus> = connectivityService.status

    override fun onViewAttached() {
        super.onViewAttached()

        launchUI {
            val reputationScore: ReputationScoreVO? = withContext(IODispatcher) {
                userProfileServiceFacade.getSelectedUserProfile()?.let {
                    // _reputation.value = it.reputation // TODO reputation?
                    setProfileAge(it)
                    setProfileId(it)
                    setBotId(it)
                    setNickname(it)
                }
                userRepository.fetch()?.let {
                    // The following should be local to the app
                    setLastActivity(it)
                    setTradeTerms(it)
                    setStatement(it)
                }

                reputationServiceFacade.getReputation(profileId.value).getOrNull()
            }

            _reputation.value = reputationScore?.totalScore?.toString() ?: DEFAULT_UNKNOWN_VALUE

            val user = withContext(IODispatcher) { userRepository.fetch() }
            user?.let { _uniqueAvatar.value = it.uniqueAvatar }
        }
    }

    private fun setStatement(user: User) {
        _statement.value = user.statement ?: DEFAULT_UNKNOWN_VALUE
    }

    private fun setTradeTerms(user: User) {
        _tradeTerms.value = user.tradeTerms ?: DEFAULT_UNKNOWN_VALUE
    }

    private fun setLastActivity(user: User) {
        _lastUserActivity.value = user.lastActivity?.let { DateUtils.lastSeen(it) } ?: DEFAULT_UNKNOWN_VALUE
    }

    private fun setBotId(userProfile: UserProfileVO) {
        _botId.value = userProfile.nym ?: DEFAULT_UNKNOWN_VALUE
    }

    private fun setNickname(userProfile: UserProfileVO) {
        _nickname.value = userProfile.nickName ?: DEFAULT_UNKNOWN_VALUE
    }

    private fun setProfileId(userProfile: UserProfileVO) {
        _profileId.value = userProfile.id ?: DEFAULT_UNKNOWN_VALUE
    }

    private fun setProfileAge(userProfile: UserProfileVO) {
        userProfile.publishDate.let { pd ->
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
        disableInteractive()
        setShowLoading(true)
        launchUI {
            try {
                withContext(IODispatcher) {
                    userRepository.fetch()?.let { user ->
                        user.statement = statement.value
                        user.tradeTerms = tradeTerms.value
                        userRepository.update(user)

                        // avoid flicker
                        delay(500L)
                    }
                    showSnackbar("Save success") // TODO: i18n
                }
            } catch (e: Exception) {
                showSnackbar("Save failure") // TODO: i18n
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