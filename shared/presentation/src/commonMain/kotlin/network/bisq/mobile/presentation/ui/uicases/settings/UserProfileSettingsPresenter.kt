package network.bisq.mobile.presentation.ui.uicases.settings

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVOExtension.id
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
        /**
         * Get localized "N/A" value
         */
        fun getLocalizedNA(): String = "data.na".i18n()
    }

    private val selectedUserProfile: Flow<UserProfileVO?>
        get() =
            userProfileServiceFacade.selectedUserProfile


    override val uniqueAvatar: StateFlow<PlatformImage?> =
        userRepository.data.map { it.uniqueAvatar }.stateIn(
            presenterScope,
            SharingStarted.Lazily,
            null,
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    override val reputation: StateFlow<String> = selectedUserProfile
        .distinctUntilChangedBy { it?.id }
        .mapLatest {
            it?.let { profile ->
                reputationServiceFacade.getReputation(profile.id)
                    .getOrNull()?.totalScore?.toString()
            }
        }
        .flowOn(IODispatcher)
        .catch { emit(null) }
        .map { it ?: getLocalizedNA() }.stateIn(
            presenterScope,
            SharingStarted.Lazily,
            getLocalizedNA(),
        )

    override val lastUserActivity: StateFlow<String> =
        userRepository.data.map { it.lastActivity?.let { ts -> DateUtils.lastSeen(ts) } }
            .map { it ?: getLocalizedNA() }.stateIn(
                presenterScope,
                SharingStarted.Lazily,
                getLocalizedNA(),
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    override val profileAge: StateFlow<String> = selectedUserProfile
        .distinctUntilChangedBy { it?.id }
        .mapLatest {
            it?.let { profile ->
                reputationServiceFacade.getProfileAge(profile.id)
                    .getOrNull()
            }
        }
        .flowOn(IODispatcher)
        .catch { emit(null) }
        .map { age ->
            if (age != null) {
                DateUtils.formatProfileAge(age)
            } else {
                null
            }
        }.map { it ?: getLocalizedNA() }.stateIn(
            presenterScope,
            SharingStarted.Lazily,
            getLocalizedNA(),
        )

    override val profileId: StateFlow<String> =
        selectedUserProfile.map { it?.id ?: getLocalizedNA() }
            .stateIn(
                presenterScope,
                SharingStarted.Lazily,
                getLocalizedNA(),
            )

    override val nickname: StateFlow<String> =
        selectedUserProfile.map { it?.nickName ?: getLocalizedNA() }
            .stateIn(
                presenterScope,
                SharingStarted.Lazily,
                getLocalizedNA(),
            )

    override val botId: StateFlow<String> =
        selectedUserProfile.map { it?.nym ?: getLocalizedNA() }
            .stateIn(
                presenterScope,
                SharingStarted.Lazily,
                getLocalizedNA(),
            )

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
    override fun onDelete() {
        TODO("Not yet implemented")
    }

    override fun onSave() {
        disableInteractive()
        setShowLoading(true)
        launchUI {
            try {
                val na = getLocalizedNA()
                val safeStatement = statement.value.takeUnless { it == na } ?: ""
                val safeTerms = tradeTerms.value.takeUnless { it == na } ?: ""
                val result = withContext(IODispatcher) {
                    userProfileServiceFacade.updateAndPublishUserProfile(
                        safeStatement,
                        safeTerms
                    )
                }
                if (result.isSuccess) {
                    val updatedLastActivity = withContext(IODispatcher) {
                        runCatching { userRepository.updateLastActivity() }.isSuccess
                    }
                    showSnackbar("mobile.settings.userProfile.saveSuccess".i18n(), isError = false)
                    if (!updatedLastActivity) {
                        log.w { "updateLastActivity failed after successful profile save" }
                    }
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
        _showLoading.value = show
    }
}