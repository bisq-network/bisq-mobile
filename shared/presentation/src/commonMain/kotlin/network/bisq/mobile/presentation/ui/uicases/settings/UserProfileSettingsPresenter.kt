package network.bisq.mobile.presentation.ui.uicases.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
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

    private val selectedUserProfile: Flow<UserProfileVO?> =
        userProfileServiceFacade.selectedUserProfile.map { it }


    override val uniqueAvatar: StateFlow<PlatformImage?> =
        userRepository.data.map { it.uniqueAvatar }.stateIn(
            presenterScope,
            SharingStarted.Lazily,
            null,
        )

    override val reputation: StateFlow<String> =
        selectedUserProfile.map {
            it?.let { profile ->
                withContext(IODispatcher) {
                    reputationServiceFacade.getReputation(profile.id)
                        .getOrNull()?.totalScore?.toString()
                }
            }
        }.map { it ?: getLocalizedNA() }.stateIn(
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

    override val profileAge: StateFlow<String> = selectedUserProfile.map {
        it?.let { profile ->
            withContext(IODispatcher) {
                reputationServiceFacade.getProfileAge(profile.id)
                    .getOrNull()
            }
        }
    }.map {
        if (it != null) {
            DateUtils.formatProfileAge(it)
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

    override val tradeTerms: StateFlow<String> =
        selectedUserProfile.map { it?.terms ?: "" }
            .stateIn(
                presenterScope,
                SharingStarted.Lazily,
                "",
            )

    override val statement: StateFlow<String> =
        selectedUserProfile.map { it?.statement ?: "" }
            .stateIn(
                presenterScope,
                SharingStarted.Lazily,
                "",
            )


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
                val safeStatement = statement.value.takeUnless { it == DEFAULT_UNKNOWN_VALUE } ?: ""
                val safeTerms = tradeTerms.value.takeUnless { it == DEFAULT_UNKNOWN_VALUE } ?: ""
                val result = withContext(IODispatcher) {
                    userProfileServiceFacade.updateAndPublishUserProfile(
                        safeStatement,
                        safeTerms
                    )
                }
                if (result.isSuccess) {
                    withContext(IODispatcher) { userRepository.updateLastActivity() }
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
        launchIO {
            userRepository.updateTerms(it)
        }
    }

    override fun updateStatement(it: String) {
        launchIO {
            userRepository.updateStatement(it)
        }
    }

    private fun setShowLoading(show: Boolean = true) {
        launchUI {
            _showLoading.value = show
        }
    }
}